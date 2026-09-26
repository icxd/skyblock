package main

import (
	"fmt"
	"net"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/charmbracelet/bubbles/spinner"
	"github.com/charmbracelet/bubbles/textinput"
	"github.com/charmbracelet/bubbles/viewport"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/huh"
	"github.com/charmbracelet/lipgloss"
)

// The dashboard: servers with live status on top, the selected server's console below.

type mode int

const (
	modeNormal mode = iota
	modeInput
	modeForm
	modeConfirm
)

type (
	statusMsg []Status
	logMsg    struct {
		name  string
		lines []string
	}
	mongoMsg    bool
	javaMsg     string
	progressMsg struct {
		ch   chan tea.Msg
		text string
	}
	taskDoneMsg struct {
		label string
		err   error
	}
	tickMsg    time.Time
	logTickMsg time.Time
)

type model struct {
	dir      string
	net      *Network
	width    int
	height   int
	statuses []Status
	notes    map[string]string // last note logged per server
	samplers *Samplers
	cursor   int

	logs     viewport.Model
	follow   bool
	logLines []string

	input   textinput.Model
	spinner spinner.Model
	mode    mode

	form     *huh.Form
	formDone func() tea.Cmd

	confirmText string
	confirmDo   func() tea.Cmd

	busy      int
	exclusive bool // a task is changing the server list; status refreshes wait
	activity  []string
	java      string
	mongo     *bool
	showHelp  bool
}

func runTUI(dir string) error {
	m := &model{dir: dir, samplers: &Samplers{}, follow: true}
	m.spinner = spinner.New(spinner.WithSpinner(spinner.MiniDot), spinner.WithStyle(lipgloss.NewStyle().Foreground(accent)))
	m.input = textinput.New()
	m.input.Prompt = ""
	m.input.CharLimit = 1000
	m.logs = viewport.New(80, 10)
	if n, err := loadNetwork(dir); err == nil {
		m.net = n
	} else if !networkExists(dir) {
		m.startInitForm()
	} else {
		return err
	}
	_, err := tea.NewProgram(m, tea.WithAltScreen(), tea.WithMouseCellMotion()).Run()
	if m.net != nil {
		fmt.Println("The servers keep running. servermgr stop all stops them.")
	}
	return err
}

func (m *model) Init() tea.Cmd {
	cmds := []tea.Cmd{m.spinner.Tick, tick(), logTick()}
	if m.form != nil {
		cmds = append(cmds, m.form.Init())
	}
	if m.net != nil {
		cmds = append(cmds, m.refresh(), m.checkJava(), m.checkMongo())
	}
	return tea.Batch(cmds...)
}

func tick() tea.Cmd { return tea.Tick(2*time.Second, func(t time.Time) tea.Msg { return tickMsg(t) }) }
func logTick() tea.Cmd {
	return tea.Tick(500*time.Millisecond, func(t time.Time) tea.Msg { return logTickMsg(t) })
}

func (m *model) refresh() tea.Cmd {
	n := m.net
	if n == nil || m.exclusive {
		return nil
	}
	return func() tea.Msg { return statusMsg(n.collect(m.samplers)) }
}

func (m *model) checkJava() tea.Cmd {
	java := m.net.Java
	return func() tea.Msg {
		major, v, err := javaVersion(java)
		switch {
		case err != nil:
			return javaMsg("java not found")
		case major < 25:
			return javaMsg("Java " + v + " (needs 25+)")
		}
		return javaMsg("Java " + v)
	}
}

func (m *model) checkMongo() tea.Cmd {
	uri := m.net.MongoURI
	return func() tea.Msg { return mongoMsg(mongoReachable(uri)) }
}

// mongoReachable checks that something listens at the first host of a mongodb:// URI.
func mongoReachable(uri string) bool {
	rest := strings.TrimPrefix(strings.TrimPrefix(uri, "mongodb+srv://"), "mongodb://")
	if at := strings.LastIndex(rest, "@"); at >= 0 {
		rest = rest[at+1:]
	}
	if i := strings.IndexAny(rest, "/?"); i >= 0 {
		rest = rest[:i]
	}
	host := strings.Split(rest, ",")[0]
	if !strings.Contains(host, ":") {
		host += ":27017"
	}
	conn, err := net.DialTimeout("tcp", host, 2*time.Second)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

func (m *model) selected() string {
	if m.net == nil {
		return ""
	}
	names := m.net.names()
	if m.cursor >= len(names) {
		m.cursor = len(names) - 1
	}
	return names[m.cursor]
}

func (m *model) readLogs() tea.Cmd {
	if m.net == nil {
		return nil
	}
	name := m.selected()
	t, err := m.net.target(name)
	if err != nil {
		return nil
	}
	path := filepath.Join(t.dir, consoleFileName)
	return func() tea.Msg { return logMsg{name: name, lines: tail(path, 400)} }
}

// task runs fn in the background, showing its progress lines in the activity panel.
func (m *model) task(label string, exclusive bool, fn func(progress func(string)) error) tea.Cmd {
	ch := make(chan tea.Msg, 64)
	m.busy++
	if exclusive {
		m.exclusive = true
	}
	m.log(label + "...")
	go func() {
		err := fn(func(s string) { ch <- progressMsg{ch: ch, text: s} })
		ch <- taskDoneMsg{label: label, err: err}
		close(ch)
	}()
	return listen(ch)
}

func listen(ch chan tea.Msg) tea.Cmd {
	return func() tea.Msg {
		msg, ok := <-ch
		if !ok {
			return nil
		}
		return msg
	}
}

func (m *model) log(line string) {
	m.activity = append(m.activity, time.Now().Format("15:04:05")+"  "+line)
	if len(m.activity) > 200 {
		m.activity = m.activity[len(m.activity)-200:]
	}
}

func (m *model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		if m.form != nil {
			m.form = m.form.WithWidth(min(msg.Width-4, 90))
		}
		m.layout()
		return m, nil
	case spinner.TickMsg:
		var cmd tea.Cmd
		m.spinner, cmd = m.spinner.Update(msg)
		return m, cmd
	case tickMsg:
		cmds := []tea.Cmd{tick(), m.refresh()}
		if m.net != nil && time.Time(msg).Second()%10 < 2 {
			cmds = append(cmds, m.checkMongo())
		}
		return m, tea.Batch(cmds...)
	case logTickMsg:
		return m, tea.Batch(logTick(), m.readLogs())
	case statusMsg:
		m.statuses = msg
		for _, st := range msg {
			if st.Note != "" && m.notes[st.Name] != st.Note {
				m.log("! " + st.Name + ": " + st.Note)
			}
			if m.notes == nil {
				m.notes = map[string]string{}
			}
			m.notes[st.Name] = st.Note
		}
		return m, nil
	case javaMsg:
		m.java = string(msg)
		return m, nil
	case mongoMsg:
		ok := bool(msg)
		m.mongo = &ok
		return m, nil
	case logMsg:
		if msg.name == m.selected() {
			m.logLines = msg.lines
			m.logs.SetContent(colorLog(msg.lines, m.logs.Width))
			if m.follow {
				m.logs.GotoBottom()
			}
		}
		return m, nil
	case progressMsg:
		m.log(msg.text)
		return m, listen(msg.ch)
	case taskDoneMsg:
		m.busy--
		m.exclusive = false
		if msg.err != nil {
			m.log("! " + msg.label + ": " + msg.err.Error())
		} else {
			m.log(msg.label + ": done")
		}
		return m, m.refresh()
	case tea.MouseMsg:
		var cmd tea.Cmd
		m.logs, cmd = m.logs.Update(msg)
		if msg.Action == tea.MouseActionPress && (msg.Button == tea.MouseButtonWheelUp) {
			m.follow = false
		}
		return m, cmd
	}

	switch m.mode {
	case modeForm:
		return m.updateForm(msg)
	case modeInput:
		return m.updateInput(msg)
	case modeConfirm:
		if key, ok := msg.(tea.KeyMsg); ok {
			m.mode = modeNormal
			if key.String() == "y" || key.String() == "Y" {
				return m, m.confirmDo()
			}
			m.log("Cancelled")
		}
		return m, nil
	}
	if key, ok := msg.(tea.KeyMsg); ok {
		return m.updateKeys(key)
	}
	return m, nil
}

func (m *model) confirm(question string, do func() tea.Cmd) {
	m.mode = modeConfirm
	m.confirmText = question
	m.confirmDo = do
}

func (m *model) updateKeys(key tea.KeyMsg) (tea.Model, tea.Cmd) {
	if m.net == nil {
		if key.String() == "q" || key.String() == "ctrl+c" {
			return m, tea.Quit
		}
		return m, nil
	}
	n := m.net
	name := m.selected()
	switch key.String() {
	case "q", "ctrl+c":
		return m, tea.Quit
	case "up", "k":
		if m.cursor > 0 {
			m.cursor--
			m.follow = true
			return m, m.readLogs()
		}
	case "down", "j":
		if m.cursor < len(n.names())-1 {
			m.cursor++
			m.follow = true
			return m, m.readLogs()
		}
	case "enter", ":", "/":
		m.mode = modeInput
		m.input.SetValue("")
		m.input.Focus()
		return m, textinput.Blink
	case "s":
		return m, m.task("Start "+name, false, func(p func(string)) error { return n.startAndWait(name, p) })
	case "x":
		return m, m.task("Stop "+name, false, func(p func(string)) error { return n.stop(name, 90*time.Second) })
	case "r":
		return m, m.task("Restart "+name, false, func(p func(string)) error { return n.restart(name, p) })
	case "K":
		m.confirm("Kill "+name+" right away? Unsaved world changes are lost. [y/N]", func() tea.Cmd {
			return m.task("Kill "+name, false, func(p func(string)) error { return n.kill(name) })
		})
	case "a":
		return m, m.task("Start all", false, func(p func(string)) error {
			for _, s := range n.Servers {
				if !n.state(s.Name).Alive {
					if err := n.startAndWait(s.Name, p); err != nil {
						p("! " + err.Error())
					}
				}
			}
			if !n.state(proxyDirName).Alive {
				return n.startAndWait(proxyDirName, p)
			}
			return nil
		})
	case "z":
		m.confirm("Stop every server and the proxy? [y/N]", func() tea.Cmd {
			return m.task("Stop all", false, func(p func(string)) error {
				for _, name := range n.names() {
					if n.state(name).Alive {
						p("Stopping " + name)
						if err := n.stop(name, 90*time.Second); err != nil {
							p("! " + err.Error())
						}
					}
				}
				return nil
			})
		})
	case "n":
		m.startCreateForm()
		return m, m.form.Init()
	case "d":
		return m, m.task("Deploy", false, func(p func(string)) error { return n.deploy(DeployOptions{Build: true}, p) })
	case "D":
		m.confirm("Build, deploy and restart everything that's running? [y/N]", func() tea.Cmd {
			return m.task("Deploy and restart", false, func(p func(string)) error {
				return n.deploy(DeployOptions{Build: true, Restart: true}, p)
			})
		})
	case "u":
		return m, m.task("Update", true, func(p func(string)) error { return n.update(p) })
	case "delete", "backspace":
		if name == proxyDirName {
			m.log("! The proxy stays; remove servers instead")
			return m, nil
		}
		m.confirm("Remove "+name+" and delete its folder? [y/N]", func() tea.Cmd {
			return m.task("Remove "+name, true, func(p func(string)) error {
				err := n.removeServer(name, false, p)
				m.cursor = 0
				return err
			})
		})
	case "f":
		m.follow = !m.follow
		if m.follow {
			m.logs.GotoBottom()
		}
	case "pgup":
		m.follow = false
		m.logs.HalfPageUp()
	case "pgdown":
		m.logs.HalfPageDown()
	case "?":
		m.showHelp = !m.showHelp
		m.layout()
	}
	return m, nil
}

func (m *model) updateInput(msg tea.Msg) (tea.Model, tea.Cmd) {
	if key, ok := msg.(tea.KeyMsg); ok {
		switch key.String() {
		case "esc":
			m.mode = modeNormal
			m.input.Blur()
			return m, nil
		case "enter":
			command := strings.TrimPrefix(strings.TrimSpace(m.input.Value()), "/")
			m.mode = modeNormal
			m.input.Blur()
			if command == "" {
				return m, nil
			}
			n, name := m.net, m.selected()
			return m, m.task(name+"> "+command, false, func(p func(string)) error {
				out, err := n.command(name, command)
				if err != nil {
					return err
				}
				for _, line := range strings.Split(strings.TrimRight(out, "\n"), "\n") {
					if line != "" {
						p("  " + line)
					}
				}
				return nil
			})
		}
	}
	var cmd tea.Cmd
	m.input, cmd = m.input.Update(msg)
	return m, cmd
}

func (m *model) updateForm(msg tea.Msg) (tea.Model, tea.Cmd) {
	if key, ok := msg.(tea.KeyMsg); ok && key.String() == "ctrl+c" {
		return m, tea.Quit
	}
	f, cmd := m.form.Update(msg)
	m.form = f.(*huh.Form)
	switch m.form.State {
	case huh.StateCompleted:
		done := m.formDone
		m.form, m.mode = nil, modeNormal
		return m, tea.Batch(cmd, done())
	case huh.StateAborted:
		m.form, m.mode = nil, modeNormal
		if m.net == nil {
			return m, tea.Quit
		}
		m.log("Cancelled")
		return m, nil
	}
	return m, cmd
}

func (m *model) startInitForm() {
	o := &InitOptions{Dir: m.dir, Java: defaultJava(), Repo: findRepo("."), MongoURI: "mongodb://localhost:27017", MongoDatabase: "dungeons"}
	port := strconv.Itoa(defaultProxyPort)
	hub := true
	abs, _ := filepath.Abs(m.dir)
	m.form = huh.NewForm(
		huh.NewGroup(
			huh.NewNote().Title("New SkyBlock network").Description("Sets up a Velocity proxy in "+abs+".\nServers you add are wired to it automatically."),
			huh.NewInput().Title("Java").Description("A JDK 25 java executable").Value(&o.Java).Validate(func(s string) error {
				if major, v, err := javaVersion(s); err != nil {
					return err
				} else if major < 25 {
					return fmt.Errorf("that's Java %s; Paper 26.2 needs 25+", v)
				}
				return nil
			}),
			huh.NewInput().Title("SkyBlock repository").Description("For building and deploying the plugins").Value(&o.Repo),
			huh.NewInput().Title("Dungeon data").Description("skyblock-dungeon-data checkout (optional; its items/ go to every server, rooms/ to dungeon servers)").Value(&o.DungeonData),
		),
		huh.NewGroup(
			huh.NewInput().Title("MongoDB").Value(&o.MongoURI),
			huh.NewInput().Title("Database").Value(&o.MongoDatabase),
			huh.NewInput().Title("Proxy port").Description("The port players connect to").Value(&port).Validate(func(s string) error {
				if p, err := strconv.Atoi(s); err != nil || p < 1 || p > 65535 {
					return fmt.Errorf("not a port")
				}
				return nil
			}),
			huh.NewConfirm().Title("Accept the Minecraft EULA?").Description("https://aka.ms/MinecraftEULA; the servers won't run without it").Value(&o.AcceptEULA).Validate(func(b bool) error {
				if !b {
					return fmt.Errorf("the servers can't run without it")
				}
				return nil
			}),
			huh.NewConfirm().Title("Create a hub server (hub01) too?").Value(&hub),
		),
	).WithTheme(huh.ThemeCharm()).WithWidth(90)
	m.mode = modeForm
	m.formDone = func() tea.Cmd {
		o.ProxyPort, _ = strconv.Atoi(port)
		return m.task("Set up the network", true, func(p func(string)) error {
			n, err := initNetwork(*o, p)
			if err != nil {
				return err
			}
			m.net = n
			if hub {
				if _, err := n.createServer(CreateOptions{Name: "hub01", Type: "LOBBY", Memory: "2G"}, p); err != nil {
					return err
				}
			}
			p("Press a to start everything")
			return nil
		})
	}
}

func (m *model) startCreateForm() {
	o := &CreateOptions{Type: "LOBBY", Memory: "2G"}
	start := true
	var options []huh.Option[string]
	for _, t := range serverTypes {
		label := map[string]string{"LOBBY": "LOBBY: the hub", "DUNGEON_HUB": "DUNGEON_HUB: where runs end", "DUNGEONS": "DUNGEONS: dungeon runs", "CRIMSON_ISLE": "CRIMSON_ISLE",
			"DWARVEN_MINES": "DWARVEN_MINES", "NONE": "NONE: development, runs everything"}[t]
		options = append(options, huh.NewOption(label, t))
	}
	n := m.net
	m.form = huh.NewForm(
		huh.NewGroup(
			huh.NewInput().Title("Name").Description("Also its name on the proxy and in the plugin").Placeholder("dungeon01").Value(&o.Name).
				Validate(func(s string) error { return validateName(n, s) }),
			huh.NewSelect[string]().Title("Type").Options(options...).Value(&o.Type),
			huh.NewSelect[string]().Title("Memory").Options(huh.NewOptions("1G", "2G", "3G", "4G", "6G", "8G")...).Value(&o.Memory),
			huh.NewConfirm().Title("Start it right away?").Value(&start),
		),
	).WithTheme(huh.ThemeCharm()).WithWidth(min(max(m.width-4, 40), 90))
	m.mode = modeForm
	m.formDone = func() tea.Cmd {
		return m.task("Create "+o.Name, true, func(p func(string)) error {
			s, err := n.createServer(*o, p)
			if err != nil || !start {
				return err
			}
			m.exclusive = false
			return n.startAndWait(s.Name, p)
		})
	}
}

// Layout and rendering

var (
	accent    = lipgloss.Color("#F5A623")
	subtle    = lipgloss.Color("#626262")
	headerBar = lipgloss.NewStyle().Background(lipgloss.Color("#1F2937")).Foreground(lipgloss.Color("#E5E7EB")).Padding(0, 1)
	titleText = lipgloss.NewStyle().Foreground(accent).Bold(true)
	panel     = lipgloss.NewStyle().Border(lipgloss.RoundedBorder()).BorderForeground(subtle).Padding(0, 1)
	panelHead = lipgloss.NewStyle().Foreground(accent).Bold(true)
	selRow    = lipgloss.NewStyle().Background(lipgloss.Color("#374151")).Bold(true)
	helpKey   = lipgloss.NewStyle().Foreground(accent)
	helpText  = lipgloss.NewStyle().Foreground(subtle)
)

const activityLines = 5

func (m *model) layout() {
	if m.width == 0 {
		return
	}
	rows := 0
	if m.net != nil {
		rows = len(m.net.names())
	}
	used := 1 /* header */ + rows + 4 /* table panel */ + activityLines + 3 /* activity panel */ + 1 /* footer */ + 3 /* log panel frame */ + 1 /* input */
	if m.showHelp {
		used++ // the longer key list wraps onto a second line
	}
	h := m.height - used
	if h < 3 {
		h = 3
	}
	m.logs.Width = m.width - 4
	m.logs.Height = h
	if m.logLines != nil {
		m.logs.SetContent(colorLog(m.logLines, m.logs.Width))
		if m.follow {
			m.logs.GotoBottom()
		}
	}
}

func (m *model) View() string {
	if m.width == 0 {
		return ""
	}
	if m.mode == modeForm && m.form != nil {
		return lipgloss.JoinVertical(lipgloss.Left, m.header(), "", lipgloss.NewStyle().Padding(1, 2).Render(m.form.View()), m.activityPanel())
	}
	if m.net == nil {
		return lipgloss.JoinVertical(lipgloss.Left, m.header(), m.activityPanel())
	}
	name := m.selected()
	logTitle := panelHead.Render("console · "+name) + helpText.Render(map[bool]string{true: "  following", false: "  scrolled (f to follow)"}[m.follow])
	logPanel := panel.Width(m.width - 2).Render(logTitle + "\n" + m.logs.View())
	parts := []string{m.header(), m.serverTable(), m.activityPanel(), logPanel, m.inputLine(), m.footer()}
	return lipgloss.JoinVertical(lipgloss.Left, parts...)
}

func (m *model) header() string {
	left := titleText.Render("◆ SkyBlock network")
	var info []string
	if m.net != nil {
		info = append(info, m.net.Dir)
		if m.java != "" {
			info = append(info, m.java)
		}
		switch {
		case m.mongo == nil:
			info = append(info, "MongoDB …")
		case *m.mongo:
			info = append(info, "MongoDB "+okStyle.Render("●"))
		default:
			info = append(info, "MongoDB "+errStyle.Render("● unreachable"))
		}
		info = append(info, "Paper "+m.net.PaperVersion+" · Velocity "+m.net.VelocityVersion)
	}
	right := strings.Join(info, helpText.Render("  │  "))
	// Long folder paths: keep the end of it so the header stays on one line.
	for len(info) > 0 && lipgloss.Width(left)+lipgloss.Width(right)+4 > m.width {
		dir := []rune(info[0])
		if len(dir) <= 12 {
			info = info[1:]
		} else {
			info[0] = "…" + string(dir[len(dir)-max(12, len(dir)-(lipgloss.Width(left)+lipgloss.Width(right)+4-m.width)-1):])
		}
		right = strings.Join(info, helpText.Render("  │  "))
	}
	gap := m.width - lipgloss.Width(left) - lipgloss.Width(right) - 2
	if gap < 1 {
		gap = 1
	}
	return headerBar.Width(m.width).Render(left + strings.Repeat(" ", gap) + right)
}

func (m *model) serverTable() string {
	byName := map[string]Status{}
	for _, s := range m.statuses {
		byName[s.Name] = s
	}
	head := fmt.Sprintf("  %-14s %-14s %-6s %-11s %-9s %-6s %-6s %-9s %-5s %s", "NAME", "TYPE", "PORT", "STATE", "PLAYERS", "TPS", "MSPT", "MEMORY", "CPU", "UPTIME")
	lines := []string{panelHead.Render("servers"), helpText.Render(head)}
	width := m.width - 6
	for i, name := range m.net.names() {
		st, ok := byName[name]
		if !ok {
			st = Status{Name: name, State: "…"}
			if s := m.net.server(name); s != nil {
				st.Type, st.Port = s.Type, s.Port
			} else {
				st.Type, st.Port = "VELOCITY", m.net.Proxy.Port
			}
		}
		players, tps, mspt, mem, cpu := "", "", "", "", ""
		if st.State == "running" {
			players = strconv.Itoa(st.Players)
			if st.MaxPlayers > 0 {
				players += "/" + strconv.Itoa(st.MaxPlayers)
			}
			if st.TPS > 0 {
				tps = tpsStyle(st.TPS).Render(fmt.Sprintf("%-6.1f", st.TPS))
			}
			if st.MSPT > 0 {
				mspt = fmt.Sprintf("%.1f", st.MSPT)
			}
		}
		if st.MemoryMB > 0 {
			mem = fmt.Sprintf("%.0f MB", st.MemoryMB)
			cpu = fmt.Sprintf("%.0f%%", st.CPU)
		}
		if tps == "" {
			tps = strings.Repeat(" ", 6)
		}
		dot := map[string]string{"running": "●", "starting": "◐", "crashed": "✖", "stopped": "○"}[st.State]
		if dot == "" {
			dot = "·"
		}
		state := stateStyle(st.State).Render(fmt.Sprintf("%s %-9s", dot, st.State))
		marker := "  "
		if i == m.cursor {
			marker = titleText.Render("▸ ")
		}
		row := fmt.Sprintf("%-14s %-14s %-6d %s %-9s %s %-6s %-9s %-5s %s", st.Name, st.Type, st.Port, state, players, tps, mspt, mem, cpu, st.Uptime)
		if i == m.cursor {
			row = selRow.Render(row)
		}
		lines = append(lines, marker+row)
	}
	return panel.Width(width + 4).Render(strings.Join(lines, "\n"))
}

func tpsStyle(tps float64) lipgloss.Style {
	switch {
	case tps >= 19.5:
		return okStyle
	case tps >= 15:
		return warnStyle
	}
	return errStyle
}

func (m *model) activityPanel() string {
	title := panelHead.Render("activity")
	if m.busy > 0 {
		title += " " + m.spinner.View()
	}
	lines := m.activity
	if len(lines) > activityLines {
		lines = lines[len(lines)-activityLines:]
	}
	out := make([]string, 0, activityLines)
	for _, l := range lines {
		style := lipgloss.NewStyle()
		switch {
		case strings.Contains(l, "  ! "):
			style = warnStyle
		case strings.HasSuffix(l, ": done") || strings.HasSuffix(l, " is up"):
			style = okStyle
		case strings.Contains(l, "    "):
			style = dimStyle
		}
		out = append(out, style.Render(truncate(l, m.width-6)))
	}
	for len(out) < activityLines {
		out = append(out, "")
	}
	return panel.Width(m.width - 2).Render(title + "\n" + strings.Join(out, "\n"))
}

func (m *model) inputLine() string {
	switch m.mode {
	case modeInput:
		return titleText.Render(" "+m.selected()+"> ") + m.input.View()
	case modeConfirm:
		return warnStyle.Render(" " + m.confirmText)
	}
	return helpText.Render(" enter: run a command on " + m.selected())
}

func (m *model) footer() string {
	keys := [][2]string{{"↑↓", "select"}, {"s", "start"}, {"x", "stop"}, {"r", "restart"}, {"a", "start all"},
		{"z", "stop all"}, {"n", "new server"}, {"d", "deploy"}, {"u", "update"}, {"?", "more"}, {"q", "quit"}}
	if m.showHelp {
		keys = append(keys, [2]string{"K", "kill"}, [2]string{"D", "deploy + restart"}, [2]string{"del", "remove server"},
			[2]string{"f", "follow console"}, [2]string{"pgup/pgdn", "scroll console"})
	}
	var parts []string
	for _, k := range keys {
		parts = append(parts, helpKey.Render(k[0])+" "+helpText.Render(k[1]))
	}
	return lipgloss.NewStyle().Width(m.width).Render(" " + strings.Join(parts, helpText.Render("  ·  ")))
}

// colorLog colours a server log: warnings yellow, errors red, startup green.
func colorLog(lines []string, width int) string {
	out := make([]string, len(lines))
	for i, l := range lines {
		l = truncate(stripANSI(l), width)
		switch {
		case strings.Contains(l, "ERROR") || strings.Contains(l, "SEVERE") || strings.Contains(l, "Exception"):
			out[i] = errStyle.Render(l)
		case strings.Contains(l, "WARN"):
			out[i] = warnStyle.Render(l)
		case strings.Contains(l, "Done (") || strings.Contains(l, "logged in"):
			out[i] = okStyle.Render(l)
		default:
			out[i] = l
		}
	}
	return strings.Join(out, "\n")
}

func stripANSI(s string) string {
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		if s[i] == 0x1b && i+1 < len(s) && s[i+1] == '[' {
			j := i + 2
			for j < len(s) && (s[j] < '@' || s[j] > '~') {
				j++
			}
			i = j
			continue
		}
		b.WriteByte(s[i])
	}
	return b.String()
}

func truncate(s string, width int) string {
	if width <= 1 || lipgloss.Width(s) <= width {
		return s
	}
	r := []rune(s)
	if len(r) > width-1 {
		r = r[:width-1]
	}
	return string(r) + "…"
}
