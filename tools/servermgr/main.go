// servermgr sets up and runs a local SkyBlock network: a Velocity proxy and Paper servers behind
// it. Run it without arguments for the dashboard, or with a command (servermgr help).
package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/charmbracelet/lipgloss"
)

const usage = `servermgr: runs a SkyBlock network (a Velocity proxy and Paper servers behind it).

Usage: servermgr [--dir FOLDER] [command]

With no command it opens the dashboard. The network lives in FOLDER (default: $SKYBLOCK_NETWORK,
else ./network).

Commands:
  init                       set up a new network with its proxy (asks for anything not given)
      --java PATH --repo PATH --data PATH --mongo URI --database NAME --port N --accept-eula
  create NAME --type TYPE    add a Paper server behind the proxy
      --memory 2G --start    types: LOBBY, DUNGEON_HUB, DUNGEONS, CRIMSON_ISLE, DWARVEN_MINES, NONE
  remove NAME [--keep-files] take a (stopped) server off the network
  start|stop|restart [NAME|all]
  kill NAME                  end a server right away
  status [--json]            what's running, players, TPS, memory
  logs NAME [-f] [-n 40]     a server's console output (proxy: "proxy")
  cmd NAME COMMAND...        run a console command
  console NAME               an interactive console
  deploy [--no-build] [--restart]  build the plugins and put them on every server
  update                     newest Paper, Velocity and plugin builds
`

var (
	okStyle   = lipgloss.NewStyle().Foreground(lipgloss.Color("10"))
	warnStyle = lipgloss.NewStyle().Foreground(lipgloss.Color("11"))
	errStyle  = lipgloss.NewStyle().Foreground(lipgloss.Color("9"))
	dimStyle  = lipgloss.NewStyle().Foreground(lipgloss.Color("8"))
)

func main() {
	dir := os.Getenv("SKYBLOCK_NETWORK")
	if dir == "" {
		dir = "network"
	}
	args := os.Args[1:]
	for len(args) > 0 && strings.HasPrefix(args[0], "-") {
		switch {
		case args[0] == "--dir" && len(args) > 1:
			dir, args = args[1], args[2:]
		case strings.HasPrefix(args[0], "--dir="):
			dir, args = strings.TrimPrefix(args[0], "--dir="), args[1:]
		case args[0] == "-h" || args[0] == "--help":
			fmt.Print(usage)
			return
		default:
			fail(fmt.Errorf("unknown option %s", args[0]))
		}
	}
	command := "tui"
	if len(args) > 0 {
		command, args = args[0], args[1:]
	}
	if err := run(dir, command, args); err != nil {
		fail(err)
	}
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, errStyle.Render("error: ")+err.Error())
	os.Exit(1)
}

func say(line string) {
	switch {
	case strings.HasPrefix(line, "! "):
		fmt.Println(warnStyle.Render(line))
	case strings.HasPrefix(line, "  "):
		fmt.Println(dimStyle.Render(line))
	default:
		fmt.Println(line)
	}
}

func run(dir, command string, args []string) error {
	switch command {
	case "help":
		fmt.Print(usage)
		return nil
	case "tui":
		return runTUI(dir)
	case "init":
		return cmdInit(dir, args)
	}
	n, err := loadNetwork(dir)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return fmt.Errorf("no network in %s; run servermgr init (or pass --dir)", dir)
		}
		return err
	}
	switch command {
	case "create":
		return cmdCreate(n, args)
	case "remove":
		fs := flag.NewFlagSet("remove", flag.ExitOnError)
		keep := fs.Bool("keep-files", false, "keep the server's folder")
		name, rest := firstArg(args)
		fs.Parse(rest)
		if name == "" {
			return errors.New("usage: servermgr remove NAME")
		}
		return n.removeServer(name, *keep, say)
	case "start", "stop", "restart":
		return cmdLifecycle(n, command, args)
	case "kill":
		if len(args) != 1 {
			return errors.New("usage: servermgr kill NAME")
		}
		return n.kill(args[0])
	case "status":
		return cmdStatus(n, args)
	case "logs":
		return cmdLogs(n, args)
	case "cmd":
		if len(args) < 2 {
			return errors.New("usage: servermgr cmd NAME COMMAND...")
		}
		out, err := n.command(args[0], strings.Join(args[1:], " "))
		if err != nil {
			return err
		}
		fmt.Print(strings.TrimRight(out, "\n") + "\n")
		return nil
	case "console":
		if len(args) != 1 {
			return errors.New("usage: servermgr console NAME")
		}
		return cmdConsole(n, args[0])
	case "deploy":
		fs := flag.NewFlagSet("deploy", flag.ExitOnError)
		noBuild := fs.Bool("no-build", false, "use what's already built")
		restart := fs.Bool("restart", false, "restart running servers afterwards")
		fs.Parse(args)
		return n.deploy(DeployOptions{Build: !*noBuild, Restart: *restart}, say)
	case "update":
		return n.update(say)
	}
	return fmt.Errorf("unknown command %q (servermgr help)", command)
}

// firstArg splits off a leading positional argument, so flags can come after it.
func firstArg(args []string) (string, []string) {
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		return args[0], args[1:]
	}
	return "", args
}

func cmdInit(dir string, args []string) error {
	fs := flag.NewFlagSet("init", flag.ExitOnError)
	o := InitOptions{Dir: dir}
	fs.StringVar(&o.Java, "java", defaultJava(), "java executable (JDK 25+)")
	fs.StringVar(&o.Repo, "repo", findRepo("."), "skyblock repository checkout")
	fs.StringVar(&o.DungeonData, "data", "", "dungeon data checkout (its rooms/ folder)")
	fs.StringVar(&o.MongoURI, "mongo", "mongodb://localhost:27017", "MongoDB connection string")
	fs.StringVar(&o.MongoDatabase, "database", "dungeons", "MongoDB database")
	fs.IntVar(&o.ProxyPort, "port", defaultProxyPort, "port players connect to")
	fs.BoolVar(&o.AcceptEULA, "accept-eula", false, "accept the Minecraft EULA (https://aka.ms/MinecraftEULA)")
	fs.Parse(args)
	if major, v, err := javaVersion(o.Java); err != nil {
		return err
	} else if major < 25 {
		return fmt.Errorf("%s is Java %s; Paper 26.2 needs 25 or newer (--java)", o.Java, v)
	}
	if !o.AcceptEULA {
		fmt.Print("The servers need you to accept the Minecraft EULA (https://aka.ms/MinecraftEULA). Accept? [y/N] ")
		answer, _ := bufio.NewReader(os.Stdin).ReadString('\n')
		o.AcceptEULA = strings.EqualFold(strings.TrimSpace(answer), "y")
	}
	n, err := initNetwork(o, say)
	if err != nil {
		return err
	}
	fmt.Println(okStyle.Render("Network ready in " + n.Dir))
	fmt.Println("Next: servermgr create hub01 --type LOBBY, then servermgr start all")
	return nil
}

func cmdCreate(n *Network, args []string) error {
	fs := flag.NewFlagSet("create", flag.ExitOnError)
	typ := fs.String("type", "", "LOBBY, DUNGEON_HUB, DUNGEONS, CRIMSON_ISLE, DWARVEN_MINES or NONE")
	memory := fs.String("memory", "2G", "heap size")
	start := fs.Bool("start", false, "start it once it's created")
	name, rest := firstArg(args)
	fs.Parse(rest)
	if name == "" || *typ == "" {
		return errors.New("usage: servermgr create NAME --type TYPE [--memory 2G] [--start]")
	}
	s, err := n.createServer(CreateOptions{Name: name, Type: strings.ToUpper(*typ), Memory: *memory}, say)
	if err != nil {
		return err
	}
	if *start {
		return n.startAndWait(s.Name, say)
	}
	return nil
}

// cmdLifecycle starts, stops or restarts one server or all of them.
func cmdLifecycle(n *Network, command string, args []string) error {
	targets := args
	if len(targets) == 0 || (len(targets) == 1 && targets[0] == "all") {
		targets = n.names()
		if command != "start" {
			// The proxy first, so players get a clean disconnect rather than a dead server.
			targets = append([]string{proxyDirName}, targets[1:]...)
		} else {
			targets = append(targets[1:], proxyDirName)
		}
	}
	var mu sync.Mutex
	var failures []string
	var wg sync.WaitGroup
	for _, name := range targets {
		if _, err := n.target(name); err != nil {
			return err
		}
		do := func(name string) {
			var err error
			switch command {
			case "start":
				err = n.startAndWait(name, say)
			case "stop":
				say("Stopping " + name)
				err = n.stop(name, 90*time.Second)
			case "restart":
				err = n.restart(name, say)
			}
			if err != nil {
				mu.Lock()
				failures = append(failures, name+": "+err.Error())
				mu.Unlock()
			}
		}
		if name == proxyDirName {
			wg.Wait() // after the servers when starting, alone when stopping
			do(name)
			continue
		}
		wg.Add(1)
		go func(name string) {
			defer wg.Done()
			do(name)
		}(name)
	}
	wg.Wait()
	if len(failures) > 0 {
		return errors.New(strings.Join(failures, "; "))
	}
	return nil
}

func cmdStatus(n *Network, args []string) error {
	fs := flag.NewFlagSet("status", flag.ExitOnError)
	asJSON := fs.Bool("json", false, "print JSON")
	fs.Parse(args)
	var samplers Samplers
	statuses := n.collect(&samplers)
	if *asJSON {
		out, _ := json.MarshalIndent(statuses, "", "  ")
		fmt.Println(string(out))
		return nil
	}
	fmt.Printf("%-14s %-14s %-6s %-9s %-8s %-6s %-9s %s\n", "NAME", "TYPE", "PORT", "STATE", "PLAYERS", "TPS", "MEMORY", "UPTIME")
	for _, s := range statuses {
		players, tps, mem := "", "", ""
		if s.State == "running" {
			players = fmt.Sprint(s.Players)
			if s.MaxPlayers > 0 {
				players += "/" + fmt.Sprint(s.MaxPlayers)
			}
			if s.TPS > 0 {
				tps = fmt.Sprintf("%.1f", s.TPS)
			}
		}
		if s.MemoryMB > 0 {
			mem = fmt.Sprintf("%.0f MB", s.MemoryMB)
		}
		fmt.Printf("%-14s %-14s %-6d %s %-8s %-6s %-9s %s\n", s.Name, s.Type, s.Port, stateStyle(s.State).Width(9).Render(s.State), players, tps, mem, s.Uptime)
		if s.Note != "" {
			fmt.Println(warnStyle.Render("  ! " + s.Note))
		}
	}
	return nil
}

func stateStyle(state string) lipgloss.Style {
	switch state {
	case "running":
		return okStyle
	case "starting":
		return warnStyle
	case "crashed":
		return errStyle
	}
	return dimStyle
}

func cmdLogs(n *Network, args []string) error {
	fs := flag.NewFlagSet("logs", flag.ExitOnError)
	follow := fs.Bool("f", false, "keep printing new output")
	lines := fs.Int("n", 40, "lines")
	name, rest := firstArg(args)
	fs.Parse(rest)
	t, err := n.target(name)
	if err != nil {
		return err
	}
	path := t.dir + string(os.PathSeparator) + consoleFileName
	for _, l := range tail(path, *lines) {
		fmt.Println(l)
	}
	if !*follow {
		return nil
	}
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()
	f.Seek(0, 2)
	r := bufio.NewReader(f)
	for {
		line, err := r.ReadString('\n')
		if err != nil {
			time.Sleep(300 * time.Millisecond)
			if info, serr := os.Stat(path); serr == nil {
				// Restarted: console.log starts over.
				if pos, _ := f.Seek(0, 1); info.Size() < pos {
					f.Seek(0, 0)
					r.Reset(f)
				}
			}
			continue
		}
		fmt.Print(line)
	}
}

func cmdConsole(n *Network, name string) error {
	if !n.state(name).Alive {
		return fmt.Errorf("%s isn't running", name)
	}
	fmt.Println(dimStyle.Render("Console for " + name + ". Commands run on the server; Ctrl+D or \"exit\" leaves."))
	in := bufio.NewScanner(os.Stdin)
	for {
		fmt.Print(okStyle.Render(name + "> "))
		if !in.Scan() {
			fmt.Println()
			return nil
		}
		line := strings.TrimSpace(in.Text())
		if line == "" {
			continue
		}
		if line == "exit" || line == "quit" {
			return nil
		}
		out, err := n.command(name, strings.TrimPrefix(line, "/"))
		if err != nil {
			fmt.Println(errStyle.Render(err.Error()))
			continue
		}
		if out = strings.TrimRight(out, "\n"); out != "" {
			fmt.Println(out)
		}
	}
}
