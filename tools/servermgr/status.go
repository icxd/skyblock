package main

import (
	"fmt"
	"net"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/shirou/gopsutil/v4/process"
)

// Status is a snapshot of one server for the dashboard and `status`.
type Status struct {
	Name       string   `json:"name"`
	Type       string   `json:"type"`
	Port       int      `json:"port"`
	State      string   `json:"state"` // stopped, starting, running, crashed
	PID        int      `json:"pid,omitempty"`
	Uptime     string   `json:"uptime,omitempty"`
	Players    int      `json:"players"`
	MaxPlayers int      `json:"maxPlayers,omitempty"`
	Names      []string `json:"playerNames,omitempty"`
	TPS        float64  `json:"tps,omitempty"`
	MSPT       float64  `json:"mspt,omitempty"`
	MemoryMB   float64  `json:"memoryMB,omitempty"`
	CPU        float64  `json:"cpu,omitempty"`
	Note       string   `json:"note,omitempty"` // why some details are missing
}

var (
	paperList  = regexp.MustCompile(`There are (\d+) of a max of (\d+) players online:?(.*)`)
	proxyList  = regexp.MustCompile(`(\d+) players? (?:are |is )?(?:currently )?connected`)
	firstFloat = regexp.MustCompile(`\d+(?:\.\d+)?`)
)

// Samplers keeps process handles between refreshes, so CPU use is measured since the last one.
type Samplers struct {
	mu    sync.Mutex
	procs map[int]*process.Process
}

func (s *Samplers) get(pid int) *process.Process {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.procs == nil {
		s.procs = map[int]*process.Process{}
	}
	if p, ok := s.procs[pid]; ok {
		return p
	}
	p, err := process.NewProcess(int32(pid))
	if err != nil {
		return nil
	}
	s.procs[pid] = p
	return p
}

// collect gets every server's status in parallel.
func (n *Network) collect(samplers *Samplers) []Status {
	names := n.names()
	out := make([]Status, len(names))
	var wg sync.WaitGroup
	for i, name := range names {
		wg.Add(1)
		go func(i int, name string) {
			defer wg.Done()
			out[i] = n.status(name, samplers)
		}(i, name)
	}
	wg.Wait()
	return out
}

func (n *Network) status(name string, samplers *Samplers) Status {
	st := Status{Name: name, Type: "VELOCITY", Port: n.Proxy.Port}
	if s := n.server(name); s != nil {
		st.Type, st.Port = s.Type, s.Port
	}
	state := n.state(name)
	switch {
	case state.Alive:
		st.State = "starting"
	case state.Crashed:
		st.State = "crashed"
		return st
	default:
		st.State = "stopped"
		return st
	}
	st.PID = state.PID
	st.Uptime = shortDuration(time.Since(state.Started))
	if p := samplers.get(state.PID); p != nil {
		if mem, err := p.MemoryInfo(); err == nil {
			st.MemoryMB = float64(mem.RSS) / (1 << 20)
		}
		if cpu, err := p.Percent(0); err == nil {
			st.CPU = cpu
		}
	}
	if name == proxyDirName {
		out, err := n.command(name, "glist")
		if err != nil {
			// Velocity opens its port once it's up, so it can be running without its console
			// (the proxy plugin adds that).
			if portOpen(st.Port) {
				st.State = "running"
				st.Note = "no console: " + n.proxyConsoleProblem()
			}
			return st
		}
		st.State = "running"
		if m := proxyList.FindStringSubmatch(out); m != nil {
			st.Players, _ = strconv.Atoi(m[1])
		}
		return st
	}
	out, err := n.command(name, "list")
	if err != nil {
		return st
	}
	st.State = "running"
	if m := paperList.FindStringSubmatch(out); m != nil {
		st.Players, _ = strconv.Atoi(m[1])
		st.MaxPlayers, _ = strconv.Atoi(m[2])
		for _, p := range strings.Split(m[3], ",") {
			if p = strings.TrimSpace(p); p != "" {
				st.Names = append(st.Names, p)
			}
		}
	}
	if out, err := n.command(name, "tps"); err == nil {
		if _, after, ok := strings.Cut(out, ":"); ok {
			st.TPS, _ = strconv.ParseFloat(firstFloat.FindString(after), 64)
		}
	}
	if out, err := n.command(name, "mspt"); err == nil {
		// "Server tick times (avg/min/max) from last 5s, 10s, 1m:\n◴ 1.2/0.9/5.1, ..."
		if _, after, ok := strings.Cut(out, "\n"); ok {
			st.MSPT, _ = strconv.ParseFloat(firstFloat.FindString(after), 64)
		}
	}
	return st
}

func portOpen(port int) bool {
	conn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 500*time.Millisecond)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

// proxyConsoleProblem guesses why a running proxy's console doesn't answer.
func (n *Network) proxyConsoleProblem() string {
	if jars, _ := filepath.Glob(filepath.Join(n.proxyDir(), "plugins", "skyblock-proxy-*.jar")); len(jars) == 0 {
		return "the proxy plugin isn't installed (deploy builds and installs it)"
	}
	return "the proxy plugin didn't start it (see the proxy's console)"
}

func shortDuration(d time.Duration) string {
	switch {
	case d < time.Minute:
		return strconv.Itoa(int(d.Seconds())) + "s"
	case d < time.Hour:
		return strconv.Itoa(int(d.Minutes())) + "m"
	case d < 24*time.Hour:
		return strconv.Itoa(int(d.Hours())) + "h " + strconv.Itoa(int(d.Minutes())%60) + "m"
	default:
		return strconv.Itoa(int(d.Hours()/24)) + "d " + strconv.Itoa(int(d.Hours())%24) + "h"
	}
}
