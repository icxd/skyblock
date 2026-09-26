package main

import (
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

// Server types, as the Paper plugin's config.yml calls them (common/ServerType.java).
var serverTypes = []string{"LOBBY", "DUNGEONS", "CRIMSON_ISLE", "DWARVEN_MINES", "NONE"}

const (
	configFile       = "network.json"
	proxyDirName     = "proxy"
	serversDirName   = "servers"
	cacheDirName     = "cache"
	pidFileName      = ".servermgr.pid"
	consoleFileName  = "console.log"
	firstPaperPort   = 30001
	firstRconPort    = 40001
	defaultProxyPort = 25565
)

// Network is everything the manager knows about one network, saved as network.json in its folder.
type Network struct {
	Dir string `json:"-"`

	// Java is the java executable the servers run on (and Maven builds with).
	Java string `json:"java"`
	// Repo is a checkout of the skyblock repository, for building and deploying the plugins.
	Repo string `json:"repo"`
	// DungeonData is a checkout of the dungeon data repository (its rooms/ folder is linked into dungeon servers).
	DungeonData   string `json:"dungeonData"`
	MongoURI      string `json:"mongoUri"`
	MongoDatabase string `json:"mongoDatabase"`

	PaperVersion    string `json:"paperVersion"`
	VelocityVersion string `json:"velocityVersion"`

	Proxy   *Proxy    `json:"proxy"`
	Servers []*Server `json:"servers"`
}

// Proxy is the Velocity proxy players connect to.
type Proxy struct {
	Port         int    `json:"port"`
	RconPort     int    `json:"rconPort"`
	RconPassword string `json:"rconPassword"`
	Memory       string `json:"memory"`
	Build        int    `json:"build"`
	Secret       string `json:"forwardingSecret"`
}

// Server is one Paper server behind the proxy.
type Server struct {
	Name         string `json:"name"`
	Type         string `json:"type"`
	Port         int    `json:"port"`
	RconPort     int    `json:"rconPort"`
	RconPassword string `json:"rconPassword"`
	Memory       string `json:"memory"`
	Build        int    `json:"build"`
	// FirstStartDone is set once the per-type first-start commands have run.
	FirstStartDone bool `json:"firstStartDone"`
}

var namePattern = regexp.MustCompile(`^[a-z0-9][a-z0-9_-]{0,31}$`)

// loadNetwork reads dir/network.json.
func loadNetwork(dir string) (*Network, error) {
	abs, err := filepath.Abs(dir)
	if err != nil {
		return nil, err
	}
	data, err := os.ReadFile(filepath.Join(abs, configFile))
	if err != nil {
		return nil, err
	}
	var n Network
	if err := json.Unmarshal(data, &n); err != nil {
		return nil, fmt.Errorf("%s: %w", configFile, err)
	}
	n.Dir = abs
	return &n, nil
}

func networkExists(dir string) bool {
	_, err := os.Stat(filepath.Join(dir, configFile))
	return err == nil
}

func (n *Network) save() error {
	data, err := json.MarshalIndent(n, "", "  ")
	if err != nil {
		return err
	}
	tmp := filepath.Join(n.Dir, configFile+".tmp")
	if err := os.WriteFile(tmp, append(data, '\n'), 0o600); err != nil {
		return err
	}
	return os.Rename(tmp, filepath.Join(n.Dir, configFile))
}

func (n *Network) proxyDir() string           { return filepath.Join(n.Dir, proxyDirName) }
func (n *Network) cacheDir() string           { return filepath.Join(n.Dir, cacheDirName) }
func (n *Network) serverDir(s *Server) string { return filepath.Join(n.Dir, serversDirName, s.Name) }

func (n *Network) server(name string) *Server {
	for _, s := range n.Servers {
		if s.Name == name {
			return s
		}
	}
	return nil
}

// lobbies are the servers players are sent to when they join (Velocity's try list).
func (n *Network) lobbies() []*Server {
	var out []*Server
	for _, s := range n.Servers {
		if s.Type == "LOBBY" {
			out = append(out, s)
		}
	}
	if len(out) == 0 && len(n.Servers) > 0 {
		out = append(out, n.Servers[0])
	}
	return out
}

// nextPorts picks the first game and RCON ports no server uses yet.
func (n *Network) nextPorts() (int, int) {
	used := map[int]bool{}
	if n.Proxy != nil {
		used[n.Proxy.Port] = true
		used[n.Proxy.RconPort] = true
	}
	for _, s := range n.Servers {
		used[s.Port] = true
		used[s.RconPort] = true
	}
	port := firstPaperPort
	for used[port] {
		port++
	}
	rcon := firstRconPort
	for used[rcon] {
		rcon++
	}
	return port, rcon
}

func validateName(n *Network, name string) error {
	if !namePattern.MatchString(name) {
		return errors.New("use lowercase letters, digits, - and _ (up to 32)")
	}
	if name == proxyDirName {
		return errors.New(`"proxy" is the proxy's name`)
	}
	if n != nil && n.server(name) != nil {
		return fmt.Errorf("there's already a server called %s", name)
	}
	return nil
}

func validType(t string) bool {
	for _, s := range serverTypes {
		if s == t {
			return true
		}
	}
	return false
}

// randomSecret is a random alphanumeric string for passwords and the forwarding secret.
func randomSecret(length int) string {
	const letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	var b strings.Builder
	for i := 0; i < length; i++ {
		k, err := rand.Int(rand.Reader, big.NewInt(int64(len(letters))))
		if err != nil {
			panic(err)
		}
		b.WriteByte(letters[k.Int64()])
	}
	return b.String()
}

// findRepo looks for the skyblock repository (a pom.xml with skyblock-parent) from dir upwards.
func findRepo(dir string) string {
	abs, err := filepath.Abs(dir)
	if err != nil {
		return ""
	}
	for {
		data, err := os.ReadFile(filepath.Join(abs, "pom.xml"))
		if err == nil && strings.Contains(string(data), "<artifactId>skyblock-parent</artifactId>") {
			return abs
		}
		parent := filepath.Dir(abs)
		if parent == abs {
			return ""
		}
		abs = parent
	}
}
