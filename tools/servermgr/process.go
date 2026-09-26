package main

import (
	"bufio"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/shirou/gopsutil/v4/process"
)

// State is what a server's process is doing.
type State struct {
	PID     int
	Alive   bool
	Crashed bool // it was started and died without a clean stop
	Started time.Time
}

// target is one server the manager runs: the proxy or a Paper server.
type target struct {
	name, dir, jar, memory string
	proxy                  bool
}

func (n *Network) target(name string) (*target, error) {
	if name == proxyDirName {
		return &target{name: name, dir: n.proxyDir(), jar: "velocity.jar", memory: n.Proxy.Memory, proxy: true}, nil
	}
	s := n.server(name)
	if s == nil {
		return nil, fmt.Errorf("no server called %s", name)
	}
	return &target{name: name, dir: n.serverDir(s), jar: "server.jar", memory: s.Memory}, nil
}

// names is every server, proxy first.
func (n *Network) names() []string {
	out := []string{proxyDirName}
	for _, s := range n.Servers {
		out = append(out, s.Name)
	}
	return out
}

// marker is on each server's java command line, so its process can be told apart from others.
func marker(name string) string { return "-Dservermgr.server=" + name }

func (n *Network) state(name string) State {
	t, err := n.target(name)
	if err != nil {
		return State{}
	}
	data, err := os.ReadFile(filepath.Join(t.dir, pidFileName))
	if err != nil {
		return State{}
	}
	pid, _ := strconv.Atoi(strings.TrimSpace(string(data)))
	if p := findProcess(pid, name); p != nil {
		created, _ := p.CreateTime()
		return State{PID: pid, Alive: true, Started: time.UnixMilli(created)}
	}
	return State{PID: pid, Crashed: !stoppedCleanly(filepath.Join(t.dir, consoleFileName))}
}

// findProcess is the server's java process, if it's still running.
func findProcess(pid int, name string) *process.Process {
	if pid <= 0 {
		return nil
	}
	p, err := process.NewProcess(int32(pid))
	if err != nil {
		return nil
	}
	if running, _ := p.IsRunning(); !running {
		return nil
	}
	if cmd, err := p.Cmdline(); err != nil || !strings.Contains(cmd, marker(name)) {
		return nil
	}
	return p
}

// stoppedCleanly looks at the end of the console log for a normal shutdown.
func stoppedCleanly(path string) bool {
	lines := tail(path, 40)
	for _, l := range lines {
		if strings.Contains(l, "Stopping server") || strings.Contains(l, "Stopping the server") ||
			strings.Contains(l, "Shutting down the proxy") || strings.Contains(l, "Closing endpoint") {
			return true
		}
	}
	return false
}

// javaArgs are the JVM flags: Aikar's for Paper, Velocity's recommended ones for the proxy.
func javaArgs(t *target) []string {
	args := []string{"-Xms" + t.memory, "-Xmx" + t.memory, marker(t.name)}
	if t.proxy {
		args = append(args, "-XX:+UseG1GC", "-XX:G1HeapRegionSize=4M", "-XX:+UnlockExperimentalVMOptions",
			"-XX:+ParallelRefProcEnabled", "-XX:+AlwaysPreTouch", "-XX:MaxInlineLevel=15")
	} else {
		args = append(args, "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200",
			"-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch",
			"-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M",
			"-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4",
			"-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90",
			"-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem",
			"-XX:MaxTenuringThreshold=1", "-Dusing.aikars.flags=https://mcflags.emc.gs", "-Daikars.new.flags=true")
	}
	args = append(args, "-jar", t.jar)
	if !t.proxy {
		args = append(args, "--nogui")
	}
	return args
}

// start launches a server in the background; it keeps running when the manager exits.
func (n *Network) start(name string) error {
	t, err := n.target(name)
	if err != nil {
		return err
	}
	if n.state(name).Alive {
		return fmt.Errorf("%s is already running", name)
	}
	// An update downloaded while it was running.
	if _, err := os.Stat(filepath.Join(t.dir, t.jar+".new")); err == nil {
		if err := os.Rename(filepath.Join(t.dir, t.jar+".new"), filepath.Join(t.dir, t.jar)); err != nil {
			return fmt.Errorf("applying the update: %w", err)
		}
	}
	if _, err := os.Stat(filepath.Join(t.dir, t.jar)); err != nil {
		return fmt.Errorf("%s has no %s", name, t.jar)
	}
	if name == proxyDirName {
		// Networks made before the proxy plugin had settings of its own.
		if err := n.ensureProxyConfig(); err != nil {
			return err
		}
	}
	log, err := os.Create(filepath.Join(t.dir, consoleFileName))
	if err != nil {
		return err
	}
	defer log.Close()
	cmd := exec.Command(n.Java, javaArgs(t)...)
	cmd.Dir = t.dir
	cmd.Stdout = log
	cmd.Stderr = log
	detach(cmd)
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("starting %s: %w", name, err)
	}
	if err := os.WriteFile(filepath.Join(t.dir, pidFileName), []byte(strconv.Itoa(cmd.Process.Pid)), 0o644); err != nil {
		return err
	}
	return cmd.Process.Release()
}

// stop asks a server to shut down (RCON stop, or shutdown for the proxy) and waits for it,
// killing it if it doesn't go within the timeout.
func (n *Network) stop(name string, timeout time.Duration) error {
	st := n.state(name)
	if !st.Alive {
		n.clearPID(name)
		return nil
	}
	command := "stop"
	if name == proxyDirName {
		command = "shutdown"
	}
	if _, err := n.command(name, command); err != nil {
		// No console (still starting, or the proxy plugin is missing): ask the OS nicely.
		if err := interrupt(st.PID); err != nil {
			return n.kill(name)
		}
	}
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if !n.state(name).Alive {
			n.clearPID(name)
			return nil
		}
		time.Sleep(250 * time.Millisecond)
	}
	return n.kill(name)
}

// kill ends a server right away (worlds may lose unsaved changes).
func (n *Network) kill(name string) error {
	st := n.state(name)
	if st.Alive {
		p, err := process.NewProcess(int32(st.PID))
		if err == nil {
			if err := p.Kill(); err != nil {
				return err
			}
		}
		for i := 0; i < 40 && n.state(name).Alive; i++ {
			time.Sleep(250 * time.Millisecond)
		}
	}
	n.clearPID(name)
	return nil
}

func (n *Network) clearPID(name string) {
	if t, err := n.target(name); err == nil {
		os.Remove(filepath.Join(t.dir, pidFileName))
	}
}

// waitReady waits until a server answers on its console.
func (n *Network) waitReady(name string, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if !n.state(name).Alive {
			return fmt.Errorf("%s stopped while starting; see its console.log", name)
		}
		if _, err := n.command(name, "list"); err == nil {
			return nil
		}
		if name == proxyDirName && portOpen(n.Proxy.Port) {
			return nil // up, but without its console (see proxyConsoleProblem)
		}
		time.Sleep(time.Second)
	}
	return fmt.Errorf("%s didn't come up within %s", name, timeout)
}

// command runs a console command on a server over RCON.
func (n *Network) command(name, command string) (string, error) {
	port, password := 0, ""
	if name == proxyDirName {
		port, password = n.Proxy.RconPort, n.Proxy.RconPassword
	} else if s := n.server(name); s != nil {
		port, password = s.RconPort, s.RconPassword
	} else {
		return "", fmt.Errorf("no server called %s", name)
	}
	return consoles.run(fmt.Sprintf("127.0.0.1:%d", port), password, command)
}

// consoles keeps one RCON connection per server open and reuses it: Paper logs every new
// connection, and the dashboard asks for status every couple of seconds.
var consoles = &rconPool{conns: map[string]*pooledRCON{}}

type rconPool struct {
	mu    sync.Mutex
	conns map[string]*pooledRCON
}

type pooledRCON struct {
	mu   sync.Mutex
	conn *RCON
}

func (p *rconPool) run(addr, password, command string) (string, error) {
	p.mu.Lock()
	c := p.conns[addr]
	if c == nil {
		c = &pooledRCON{}
		p.conns[addr] = c
	}
	p.mu.Unlock()
	c.mu.Lock()
	defer c.mu.Unlock()
	for attempt := 0; attempt < 2; attempt++ {
		if c.conn == nil {
			conn, err := dialRCON(addr, password, 3*time.Second)
			if err != nil {
				return "", err
			}
			c.conn = conn
		}
		out, err := c.conn.Run(command)
		if err == nil {
			return out, nil
		}
		// The server restarted or dropped us: reconnect once.
		c.conn.Close()
		c.conn = nil
		if attempt == 1 {
			return "", err
		}
	}
	return "", nil
}

// firstStart runs the per-type setup commands the first time a server is up.
func (n *Network) firstStart(s *Server) error {
	if s.FirstStartDone {
		return nil
	}
	var commands []string
	if s.Type == "DUNGEONS" || s.Type == "NONE" {
		// Hypixel's dungeons don't random-tick; with it on, ice melts and floods rooms.
		commands = append(commands, "gamerule random_tick_speed 0", "gamerule spawn_mobs false")
	}
	// Restoring inventories on join would announce vanilla advancements on every server.
	commands = append(commands, "gamerule advance_time false", "gamerule advance_weather false",
		"gamerule show_advancement_messages false")
	for _, c := range commands {
		if _, err := n.command(s.Name, c); err != nil {
			return err
		}
	}
	s.FirstStartDone = true
	return n.save()
}

var javaVersionPattern = regexp.MustCompile(`version "([^"]+)"`)

// javaVersion runs `java -version` and returns the major version and the full version string.
func javaVersion(java string) (int, string, error) {
	out, err := exec.Command(java, "-version").CombinedOutput()
	if err != nil {
		return 0, "", fmt.Errorf("%s: %w", java, err)
	}
	m := javaVersionPattern.FindStringSubmatch(string(out))
	if m == nil {
		return 0, "", errors.New("couldn't read the Java version")
	}
	v := m[1]
	major := v
	if strings.HasPrefix(v, "1.") {
		major = strings.Split(v, ".")[1]
	} else if i := strings.IndexAny(v, ".+-"); i >= 0 {
		major = v[:i]
	}
	n, _ := strconv.Atoi(major)
	return n, v, nil
}

// defaultJava is JAVA_HOME's java, else the one on PATH.
func defaultJava() string {
	if home := os.Getenv("JAVA_HOME"); home != "" {
		p := filepath.Join(home, "bin", javaExe)
		if _, err := os.Stat(p); err == nil {
			return p
		}
	}
	if p, err := exec.LookPath(javaExe); err == nil {
		return p
	}
	return javaExe
}

// tail is the last n lines of a file.
func tail(path string, n int) []string {
	f, err := os.Open(path)
	if err != nil {
		return nil
	}
	defer f.Close()
	info, err := f.Stat()
	if err != nil {
		return nil
	}
	// Enough of the end of the file for n lines of server log.
	const chunk = 256 * 1024
	offset := info.Size() - chunk
	if offset < 0 {
		offset = 0
	}
	if _, err := f.Seek(offset, 0); err != nil {
		return nil
	}
	var lines []string
	sc := bufio.NewScanner(f)
	sc.Buffer(make([]byte, 64*1024), 1024*1024)
	first := offset > 0
	for sc.Scan() {
		if first {
			first = false // probably cut off
			continue
		}
		lines = append(lines, sc.Text())
	}
	if len(lines) > n {
		lines = lines[len(lines)-n:]
	}
	return lines
}
