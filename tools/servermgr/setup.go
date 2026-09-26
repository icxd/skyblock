package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"

	"gopkg.in/yaml.v3"
)

// InitOptions are what `init` (or the TUI's first-run form) asks for.
type InitOptions struct {
	Dir           string
	Java          string
	Repo          string
	DungeonData   string
	MongoURI      string
	MongoDatabase string
	ProxyPort     int
	AcceptEULA    bool
}

// initNetwork sets up a new network folder with a Velocity proxy.
func initNetwork(o InitOptions, progress func(string)) (*Network, error) {
	dir, err := filepath.Abs(o.Dir)
	if err != nil {
		return nil, err
	}
	if networkExists(dir) {
		return nil, fmt.Errorf("%s already has a network", dir)
	}
	if !o.AcceptEULA {
		return nil, errors.New("the servers can't run without accepting the Minecraft EULA (https://aka.ms/MinecraftEULA)")
	}
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return nil, err
	}
	n := &Network{
		Dir:             dir,
		Java:            o.Java,
		Repo:            o.Repo,
		DungeonData:     o.DungeonData,
		MongoURI:        o.MongoURI,
		MongoDatabase:   o.MongoDatabase,
		PaperVersion:    "26.2",
		VelocityVersion: "4.2.0",
		Proxy: &Proxy{
			Port:         o.ProxyPort,
			RconPort:     firstRconPort - 1,
			RconPassword: randomSecret(24),
			Memory:       "512M",
			Secret:       randomSecret(24),
		},
	}
	progress("Looking up the newest Velocity " + n.VelocityVersion)
	velocity, err := latestPaperMC("velocity", n.VelocityVersion)
	if err != nil {
		return nil, err
	}
	jar, err := n.fetch(velocity, progress)
	if err != nil {
		return nil, err
	}
	if err := copyFile(jar, filepath.Join(n.proxyDir(), "velocity.jar")); err != nil {
		return nil, err
	}
	n.Proxy.Build = velocity.Build
	if err := n.writeProxyFiles(true); err != nil {
		return nil, err
	}
	n.installProxyPlugin(progress)
	progress("Proxy ready: Velocity " + n.VelocityVersion + " build " + strconv.Itoa(velocity.Build))
	return n, n.save()
}

// writeProxyFiles writes velocity.toml (all of it only when fresh; otherwise just [servers]), the
// forwarding secret, the remote console settings and the proxy plugin's config.
func (n *Network) writeProxyFiles(fresh bool) error {
	dir := n.proxyDir()
	if err := os.MkdirAll(filepath.Join(dir, "plugins", "skyblock"), 0o755); err != nil {
		return err
	}
	if err := os.WriteFile(filepath.Join(dir, "forwarding.secret"), []byte(n.Proxy.Secret), 0o600); err != nil {
		return err
	}
	console := fmt.Sprintf("# Written by servermgr: the remote console it uses to run proxy commands.\nport=%d\npassword=%s\n",
		n.Proxy.RconPort, n.Proxy.RconPassword)
	if err := os.WriteFile(filepath.Join(dir, "plugins", "skyblock", "remote-console.properties"), []byte(console), 0o600); err != nil {
		return err
	}
	if err := n.ensureProxyConfig(); err != nil {
		return err
	}
	path := filepath.Join(dir, "velocity.toml")
	if _, err := os.Stat(path); fresh || err != nil {
		if err := os.WriteFile(path, []byte(fmt.Sprintf(velocityTemplate, n.Proxy.Port)), 0o644); err != nil {
			return err
		}
	}
	return n.writeVelocityServers()
}

// velocityTemplate is Velocity 4.2's default config with modern forwarding and no example servers.
const velocityTemplate = `# Velocity config, written by servermgr. Edit anything except [servers], which servermgr
# rewrites when servers are added or removed.
config-version = "2.9"
bind = "0.0.0.0:%d"
motd = "<gold><bold>SkyBlock"
show-max-players = 500
online-mode = true
force-key-authentication = true
prevent-client-proxy-connections = false
# The backends are in offline mode behind the proxy and trust it through forwarding.secret.
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"
announce-forge = false
kick-existing-players = false
sample-players-in-ping = false
enable-player-address-logging = true

[ping-passthrough]
version = false
players = false
description = false
favicon = false
modinfo = false

[packet-limiter]
interval = 7
packets-per-second = -1
bytes-per-second = -1
decompressed-bytes-per-second = 5242880

[servers]

[forced-hosts]

[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
haproxy-protocol = false
tcp-fast-open = false
bungee-plugin-message-channel = true
show-ping-requests = false
failover-on-unexpected-server-disconnect = true
announce-proxy-commands = true
log-command-executions = false
log-player-connections = true
accepts-transfers = false
enable-reuse-port = false
command-rate-limit = 50
forward-commands-if-rate-limited = true
kick-after-rate-limited-commands = 0

[query]
enabled = false
port = 25565
map = "Velocity"
show-plugins = false
`

// writeVelocityServers replaces velocity.toml's [servers] table with the network's servers.
func (n *Network) writeVelocityServers() error {
	path := filepath.Join(n.proxyDir(), "velocity.toml")
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	lines := strings.Split(strings.ReplaceAll(string(data), "\r\n", "\n"), "\n")
	start, end := -1, len(lines)
	for i, line := range lines {
		t := strings.TrimSpace(line)
		if start < 0 && t == "[servers]" {
			start = i
		} else if start >= 0 && strings.HasPrefix(t, "[") && !strings.HasPrefix(t, "[[") {
			end = i
			break
		}
	}
	section := []string{"[servers]", "# Managed by servermgr."}
	for _, s := range n.Servers {
		section = append(section, fmt.Sprintf("%s = \"127.0.0.1:%d\"", s.Name, s.Port))
	}
	var try []string
	for _, s := range n.lobbies() {
		try = append(try, strconv.Quote(s.Name))
	}
	section = append(section, "try = ["+strings.Join(try, ", ")+"]", "")
	if start < 0 {
		lines = append(lines, section...)
	} else {
		lines = append(append(append([]string{}, lines[:start]...), section...), lines[end:]...)
	}
	return os.WriteFile(path, []byte(strings.Join(lines, "\n")), 0o644)
}

// CreateOptions are what `create` (or the TUI's new server form) asks for.
type CreateOptions struct {
	Name   string
	Type   string
	Memory string
}

// ensureProxyConfig writes the proxy plugin's config.properties with the network's MongoDB, unless
// it's there already (it's the user's to tune after that).
func (n *Network) ensureProxyConfig() error {
	path := filepath.Join(n.proxyDir(), "plugins", "skyblock", "config.properties")
	if _, err := os.Stat(path); err == nil {
		return nil
	}
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	config := fmt.Sprintf(`# SkyBlock proxy plugin settings (written by servermgr; yours to change).
# The same MongoDB as the Paper servers: server types and load, ranks, and dungeon runs come from it.
mongodb.uri=%s
mongodb.database=%s
# How many dungeon runs share one dungeon server before parties go to another (or wait in line).
dungeons.runs-per-server=4
party.max-size=10
`, n.MongoURI, n.MongoDatabase)
	return os.WriteFile(path, []byte(config), 0o600)
}

// createServer adds a Paper server, set up to sit behind the proxy.
func (n *Network) createServer(o CreateOptions, progress func(string)) (*Server, error) {
	if err := validateName(n, o.Name); err != nil {
		return nil, err
	}
	if !validType(o.Type) {
		return nil, fmt.Errorf("type must be one of %s", strings.Join(serverTypes, ", "))
	}
	if o.Memory == "" {
		o.Memory = "2G"
	}
	port, rcon := n.nextPorts()
	s := &Server{Name: o.Name, Type: o.Type, Port: port, RconPort: rcon, RconPassword: randomSecret(24), Memory: o.Memory}
	dir := n.serverDir(s)
	if _, err := os.Stat(dir); err == nil {
		return nil, fmt.Errorf("%s already exists; remove it or pick another name", dir)
	}

	progress("Looking up the newest Paper " + n.PaperVersion)
	paper, err := latestPaperMC("paper", n.PaperVersion)
	if err != nil {
		return nil, err
	}
	jar, err := n.fetch(paper, progress)
	if err != nil {
		return nil, err
	}
	if err := copyFile(jar, filepath.Join(dir, "server.jar")); err != nil {
		return nil, err
	}
	s.Build = paper.Build

	progress("Writing configs")
	if err := n.writePaperFiles(s); err != nil {
		return nil, err
	}
	n.installPaperPlugins(s, progress)
	if s.Type == "DUNGEONS" || s.Type == "NONE" {
		n.linkDungeonData(s, progress)
	}

	n.Servers = append(n.Servers, s)
	if err := n.save(); err != nil {
		return nil, err
	}
	progress("Registering with the proxy")
	if err := n.writeVelocityServers(); err != nil {
		return s, err
	}
	n.reloadProxy(progress)
	progress(fmt.Sprintf("Created %s (%s) on port %d", s.Name, s.Type, s.Port))
	return s, nil
}

// writePaperFiles writes server.properties, eula.txt, Paper's proxy settings and the plugin's
// config, keeping anything else already in them.
func (n *Network) writePaperFiles(s *Server) error {
	dir := n.serverDir(s)
	if err := os.MkdirAll(filepath.Join(dir, "config"), 0o755); err != nil {
		return err
	}
	if err := os.WriteFile(filepath.Join(dir, "eula.txt"), []byte("# Accepted through servermgr (https://aka.ms/MinecraftEULA)\neula=true\n"), 0o644); err != nil {
		return err
	}
	props := [][2]string{
		// Only reachable through the proxy.
		{"server-ip", "127.0.0.1"},
		{"server-port", strconv.Itoa(s.Port)},
		{"online-mode", "false"},
		{"enable-rcon", "true"},
		{"rcon.port", strconv.Itoa(s.RconPort)},
		{"rcon.password", s.RconPassword},
		{"broadcast-rcon-to-ops", "false"},
		{"motd", "SkyBlock " + s.Name},
		{"max-players", "100"},
		{"spawn-protection", "0"},
	}
	defaults := [][2]string{
		{"level-type", `minecraft\:flat`},
		{"generate-structures", "false"},
		{"allow-nether", "false"},
		{"view-distance", "10"},
	}
	if err := setProperties(filepath.Join(dir, "server.properties"), props, defaults); err != nil {
		return err
	}
	if err := setYAML(filepath.Join(dir, "config", "paper-global.yml"), "_version: 31\n", map[string]any{
		"proxies.velocity.enabled":     true,
		"proxies.velocity.online-mode": true,
		"proxies.velocity.secret":      n.Proxy.Secret,
	}); err != nil {
		return err
	}
	pluginConfig := filepath.Join(dir, "plugins", "dungeons", "config.yml")
	if err := os.MkdirAll(filepath.Dir(pluginConfig), 0o755); err != nil {
		return err
	}
	return setYAML(pluginConfig, "", map[string]any{
		"server.name":      s.Name,
		"server.port":      s.Port,
		"server.type":      s.Type,
		"mongodb.uri":      n.MongoURI,
		"mongodb.database": n.MongoDatabase,
	})
}

// setProperties sets keys in a .properties file (always for set, only if missing for defaults).
func setProperties(path string, set, defaults [][2]string) error {
	var lines []string
	if data, err := os.ReadFile(path); err == nil {
		lines = strings.Split(strings.TrimRight(strings.ReplaceAll(string(data), "\r\n", "\n"), "\n"), "\n")
	}
	index := map[string]int{}
	for i, line := range lines {
		if k, _, ok := strings.Cut(line, "="); ok && !strings.HasPrefix(strings.TrimSpace(line), "#") {
			index[strings.TrimSpace(k)] = i
		}
	}
	apply := func(kv [2]string, overwrite bool) {
		if i, ok := index[kv[0]]; ok {
			if overwrite {
				lines[i] = kv[0] + "=" + kv[1]
			}
			return
		}
		index[kv[0]] = len(lines)
		lines = append(lines, kv[0]+"="+kv[1])
	}
	for _, kv := range set {
		apply(kv, true)
	}
	for _, kv := range defaults {
		apply(kv, false)
	}
	return os.WriteFile(path, []byte(strings.Join(lines, "\n")+"\n"), 0o644)
}

// setYAML sets dotted keys in a YAML file, keeping the rest (and its comments). A missing file
// starts as initial.
func setYAML(path, initial string, values map[string]any) error {
	data, err := os.ReadFile(path)
	if err != nil {
		data = []byte(initial)
	}
	var doc yaml.Node
	if err := yaml.Unmarshal(data, &doc); err != nil {
		return fmt.Errorf("%s: %w", path, err)
	}
	if doc.Kind == 0 {
		doc = yaml.Node{Kind: yaml.DocumentNode, Content: []*yaml.Node{{Kind: yaml.MappingNode}}}
	}
	root := doc.Content[0]
	keys := make([]string, 0, len(values))
	for k := range values {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, key := range keys {
		node := root
		parts := strings.Split(key, ".")
		for i, part := range parts {
			child := mapValue(node, part)
			if i == len(parts)-1 {
				var v yaml.Node
				if err := v.Encode(values[key]); err != nil {
					return err
				}
				if child == nil {
					node.Content = append(node.Content, &yaml.Node{Kind: yaml.ScalarNode, Value: part}, &v)
				} else {
					*child = v
				}
				break
			}
			if child == nil || child.Kind != yaml.MappingNode {
				m := &yaml.Node{Kind: yaml.MappingNode}
				if child == nil {
					node.Content = append(node.Content, &yaml.Node{Kind: yaml.ScalarNode, Value: part}, m)
				} else {
					*child = *m
					m = child
				}
				child = m
			}
			node = child
		}
	}
	out, err := yaml.Marshal(&doc)
	if err != nil {
		return err
	}
	return os.WriteFile(path, out, 0o644)
}

func mapValue(m *yaml.Node, key string) *yaml.Node {
	for i := 0; i+1 < len(m.Content); i += 2 {
		if m.Content[i].Value == key {
			return m.Content[i+1]
		}
	}
	return nil
}

// builtJar finds a module's jar in the repository's build output, or "".
func (n *Network) builtJar(module, prefix string) string {
	if n.Repo == "" {
		return ""
	}
	matches, _ := filepath.Glob(filepath.Join(n.Repo, module, "target", prefix+"-*.jar"))
	var newest string
	var newestTime int64
	for _, m := range matches {
		if strings.HasPrefix(filepath.Base(m), "original-") {
			continue
		}
		if info, err := os.Stat(m); err == nil && info.ModTime().UnixNano() > newestTime {
			newest, newestTime = m, info.ModTime().UnixNano()
		}
	}
	return newest
}

// installJar copies a jar into a plugins folder, replacing older versions of it.
func installJar(src, pluginsDir, prefix string) error {
	old, _ := filepath.Glob(filepath.Join(pluginsDir, prefix+"*.jar"))
	for _, o := range old {
		if filepath.Base(o) != filepath.Base(src) {
			os.Remove(o)
		}
	}
	return copyFile(src, filepath.Join(pluginsDir, filepath.Base(src)))
}

// Plugins every Paper server gets from Modrinth, by slug and jar name prefix.
var modrinthPlugins = [][2]string{{"worldedit", "worldedit-bukkit-"}, {"packetevents", "packetevents-"}}

func (n *Network) installPaperPlugins(s *Server, progress func(string)) {
	plugins := filepath.Join(n.serverDir(s), "plugins")
	if jar := n.builtJar("paper", "skyblock-dungeons"); jar != "" {
		if err := installJar(jar, plugins, "skyblock-dungeons-"); err != nil {
			progress("! Couldn't install the SkyBlock plugin: " + err.Error())
		}
	} else {
		progress("! The SkyBlock plugin isn't built yet; deploy to build and install it")
	}
	for _, p := range modrinthPlugins {
		a, err := latestModrinth(p[0], n.PaperVersion)
		if err == nil {
			var path string
			if path, err = n.fetch(a, progress); err == nil {
				err = installJar(path, plugins, p[1])
			}
		}
		if err != nil {
			progress("! " + p[0] + ": " + err.Error())
		}
	}
}

func (n *Network) installProxyPlugin(progress func(string)) {
	if jar := n.builtJar("proxy", "skyblock-proxy"); jar != "" {
		if err := installJar(jar, filepath.Join(n.proxyDir(), "plugins"), "skyblock-proxy-"); err != nil {
			progress("! Couldn't install the proxy plugin: " + err.Error())
		}
	} else {
		progress("! The proxy plugin isn't built yet; deploy to build and install it (needed for its console)")
	}
}

// linkDungeonData makes the captured rooms available to a dungeon server.
func (n *Network) linkDungeonData(s *Server, progress func(string)) {
	if n.DungeonData == "" {
		progress("! No dungeon data folder configured; /dungeon paste needs plugins/dungeons/dungeon-rooms/rooms")
		return
	}
	target := filepath.Join(n.DungeonData, "rooms")
	link := filepath.Join(n.serverDir(s), "plugins", "dungeons", "dungeon-rooms", "rooms")
	if err := os.MkdirAll(filepath.Dir(link), 0o755); err != nil {
		progress("! " + err.Error())
		return
	}
	if err := linkDir(target, link); err != nil {
		progress("! Couldn't link the dungeon rooms: " + err.Error())
	}
}

// removeServer takes a stopped server off the network, and deletes its folder unless keep.
func (n *Network) removeServer(name string, keep bool, progress func(string)) error {
	s := n.server(name)
	if s == nil {
		return fmt.Errorf("no server called %s", name)
	}
	if st := n.state(s.Name); st.Alive {
		return fmt.Errorf("%s is running; stop it first", name)
	}
	var rest []*Server
	for _, o := range n.Servers {
		if o != s {
			rest = append(rest, o)
		}
	}
	n.Servers = rest
	if err := n.save(); err != nil {
		return err
	}
	if err := n.writeVelocityServers(); err != nil {
		return err
	}
	n.reloadProxy(progress)
	if !keep {
		if err := os.RemoveAll(n.serverDir(s)); err != nil {
			return err
		}
	}
	progress("Removed " + name)
	return nil
}

// reloadProxy makes a running proxy pick up velocity.toml's servers.
func (n *Network) reloadProxy(progress func(string)) {
	if !n.state(proxyDirName).Alive {
		return
	}
	if out, err := n.command(proxyDirName, "velocity reload"); err != nil {
		progress("! The proxy is running but couldn't be reloaded (" + err.Error() + "); restart it to see new servers")
	} else if strings.TrimSpace(out) != "" {
		progress("Proxy: " + firstLine(out))
	}
}

func firstLine(s string) string {
	s = strings.TrimSpace(s)
	if i := strings.IndexByte(s, '\n'); i >= 0 {
		return s[:i]
	}
	return s
}
