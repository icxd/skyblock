package main

import (
	"bytes"
	"compress/gzip"
	"encoding/binary"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"
)

// A captured Hypixel map a kind of server gets as its main world: the plugin's name for it (config.yml's
// server.map) and the area the capture's sidebar named, which is how it's found in the data checkout's
// worlds/ (the dungeon scanner's /dscan world saves).
type mapSpec struct {
	Name string
	Area string
}

// mapsByType is which map each kind of server gets when it's created.
var mapsByType = map[string]mapSpec{
	"LOBBY":       {Name: "hub", Area: "Village"},
	"DUNGEON_HUB": {Name: "dungeon_hub", Area: "Dungeon Hub"},
}

// mapNames is every map, by name.
func mapNames() map[string]mapSpec {
	out := map[string]mapSpec{}
	for _, m := range mapsByType {
		out[m.Name] = m
	}
	return out
}

// voidGenerator is server.properties' generator-settings for a flat world of nothing: where the capture has no
// chunks there's the void, as around Hypixel's islands, instead of grass at y -61.
const voidGenerator = `{"biome":"minecraft:the_void","layers":[{"block":"minecraft:air","height":1}],"features":false,"lakes":false,"structure_overrides":[]}`

// codes are Hypixel's colour codes in the capture's area names, made-up ones included ("Village§u").
var codes = regexp.MustCompile(`§.?`)

// findCapture is the newest capture (by when its saving ended) in worlds/ whose sidebar named area; "" for none.
func findCapture(data, area string) (string, error) {
	worlds := filepath.Join(data, "worlds")
	entries, err := os.ReadDir(worlds)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return "", nil
		}
		return "", err
	}
	best, bestTime := "", ""
	for _, e := range entries {
		dir := filepath.Join(worlds, e.Name())
		raw, err := os.ReadFile(filepath.Join(dir, "capture.json"))
		if err != nil || !isDir(filepath.Join(dir, "region")) {
			continue
		}
		var capture struct {
			Areas []string `json:"areas"`
			To    string   `json:"to"`
		}
		if json.Unmarshal(raw, &capture) != nil {
			continue
		}
		for _, a := range capture.Areas {
			if strings.EqualFold(strings.TrimSpace(codes.ReplaceAllString(a, "")), area) && (best == "" || capture.To > bestTime) {
				best, bestTime = dir, capture.To
			}
		}
	}
	return best, nil
}

// overworldDir is where Paper 26 keeps a server's main world's blocks and entities: its world folder's
// dimensions/minecraft/overworld.
func (n *Network) overworldDir(s *Server) string {
	return filepath.Join(n.serverDir(s), "world", "dimensions", "minecraft", "overworld")
}

// installMap makes a server's main world a captured map: the capture's region and entity files into its
// overworld, the void around it, and the plugin told which map it is. A world it had already is kept beside
// it, renamed. The server must be stopped. "none" takes the map off (a fresh flat world on the next start).
func (n *Network) installMap(s *Server, name string, progress func(string)) error {
	if n.state(s.Name).Alive {
		return fmt.Errorf("%s is running; stop it first", s.Name)
	}
	var capture string
	if name != "none" {
		spec, ok := mapNames()[name]
		if !ok {
			return fmt.Errorf("no map called %q (there's %s)", name, strings.Join(sortedMapNames(), ", "))
		}
		if n.DungeonData == "" {
			return errors.New("no data folder configured; the maps are its worlds/")
		}
		found, err := findCapture(n.DungeonData, spec.Area)
		if err != nil {
			return err
		}
		if found == "" {
			return fmt.Errorf("%s has no capture of %q in worlds/ (save one with the dungeon scanner's /dscan world)", n.DungeonData, spec.Area)
		}
		capture = found
	}
	world := filepath.Join(n.serverDir(s), "world")
	if isDir(world) {
		kept := world + "-before-" + time.Now().Format("2006-01-02_15-04-05")
		for i := 2; isDir(kept); i++ {
			kept = fmt.Sprintf("%s-before-%s-%d", world, time.Now().Format("2006-01-02_15-04-05"), i)
		}
		if err := os.Rename(world, kept); err != nil {
			return err
		}
		progress(fmt.Sprintf("%s: its world is kept as %s", s.Name, filepath.Base(kept)))
	}
	generator := ""
	if capture != "" {
		for _, folder := range []string{"region", "entities"} {
			if err := copyFolder(filepath.Join(capture, folder), filepath.Join(n.overworldDir(s), folder)); err != nil {
				return err
			}
		}
		// Paper 26 won't start a dimension that has region files but no generator settings of its own.
		settings := filepath.Join(n.overworldDir(s), "data", "minecraft", "world_gen_settings.dat")
		if err := os.MkdirAll(filepath.Dir(settings), 0o755); err != nil {
			return err
		}
		if err := os.WriteFile(settings, voidWorldGenSettings(), 0o644); err != nil {
			return err
		}
		generator = voidGenerator
		progress(fmt.Sprintf("%s: its world is now %s (%s)", s.Name, name, filepath.Base(capture)))
	}
	props := filepath.Join(n.serverDir(s), "server.properties")
	if err := setProperties(props, [][2]string{{"level-type", `minecraft\:flat`}, {"generator-settings", escapeProperty(generator)}}, nil); err != nil {
		return err
	}
	mapValue := name
	if name == "none" {
		mapValue = ""
	}
	config := filepath.Join(n.serverDir(s), "plugins", "dungeons", "config.yml")
	if err := os.MkdirAll(filepath.Dir(config), 0o755); err != nil {
		return err
	}
	if err := setYAML(config, "", map[string]any{"server.map": mapValue}); err != nil {
		return err
	}
	s.Map = mapValue
	// Its rules (no random ticks on a map) are set again on its next start.
	s.FirstStartDone = false
	return n.save()
}

// worldGenDataVersion is Minecraft 26.2's data version, which the generator settings are written in (a newer
// server upgrades them).
const worldGenDataVersion = 4903

// voidWorldGenSettings is a 26.2 world_gen_settings.dat (gzipped NBT, as a server writes it for a new world)
// whose overworld is flat with nothing in it, the other dimensions vanilla's.
func voidWorldGenSettings() []byte {
	str := func(name, v string) nbtTag { return nbtTag{8, name, v} }
	byt := func(name string, v int8) nbtTag { return nbtTag{1, name, v} }
	compound := func(name string, entries ...nbtTag) nbtTag { return nbtTag{10, name, entries} }
	noise := func(dimension, settings string, biomes ...nbtTag) nbtTag {
		return compound("minecraft:"+dimension, str("type", "minecraft:"+dimension),
			compound("generator", str("type", "minecraft:noise"), compound("biome_source", biomes...), str("settings", settings)))
	}
	root := compound("",
		nbtTag{3, "DataVersion", int32(worldGenDataVersion)},
		compound("data",
			byt("generate_structures", 0),
			compound("dimensions",
				noise("the_nether", "minecraft:nether", str("type", "minecraft:multi_noise"), str("preset", "minecraft:nether")),
				compound("minecraft:overworld", str("type", "minecraft:overworld"),
					compound("generator", str("type", "minecraft:flat"),
						compound("settings",
							nbtTag{9, "layers", []nbtTag{compound("", str("block", "minecraft:air"), nbtTag{3, "height", int32(1)})}},
							byt("features", 0), byt("lakes", 0),
							nbtTag{9, "structure_overrides", []nbtTag{}},
							str("biome", "minecraft:the_void")))),
				noise("the_end", "minecraft:end", str("type", "minecraft:the_end"))),
			nbtTag{4, "seed", int64(0)},
			byt("bonus_chest", 0)))
	var raw bytes.Buffer
	root.write(&raw, true)
	var out bytes.Buffer
	zw := gzip.NewWriter(&out)
	zw.Write(raw.Bytes())
	zw.Close()
	return out.Bytes()
}

// nbtTag is enough of NBT for voidWorldGenSettings: bytes, ints, longs, strings, compounds and lists of compounds.
type nbtTag struct {
	Type  byte
	Name  string
	Value any
}

func (t nbtTag) write(w *bytes.Buffer, named bool) {
	if named {
		w.WriteByte(t.Type)
		writeNBTString(w, t.Name)
	}
	switch v := t.Value.(type) {
	case int8:
		w.WriteByte(byte(v))
	case int32:
		binary.Write(w, binary.BigEndian, v)
	case int64:
		binary.Write(w, binary.BigEndian, v)
	case string:
		writeNBTString(w, v)
	case []nbtTag:
		if t.Type == 9 {
			kind := byte(0)
			if len(v) > 0 {
				kind = v[0].Type
			}
			w.WriteByte(kind)
			binary.Write(w, binary.BigEndian, int32(len(v)))
			for _, e := range v {
				e.write(w, false)
			}
			return
		}
		for _, e := range v {
			e.write(w, true)
		}
		w.WriteByte(0)
	}
}

func writeNBTString(w *bytes.Buffer, s string) {
	binary.Write(w, binary.BigEndian, uint16(len(s)))
	w.WriteString(s)
}

// escapeProperty writes a value the way a .properties file reads it back (its colons and the like escaped).
func escapeProperty(v string) string {
	r := strings.NewReplacer(`\`, `\\`, ":", `\:`, "=", `\=`)
	return r.Replace(v)
}

func sortedMapNames() []string {
	var names []string
	for name := range mapNames() {
		names = append(names, name)
	}
	sort.Strings(names)
	return names
}

// copyFolder copies a folder's files (not its subfolders) into another, made if missing.
func copyFolder(from, to string) error {
	entries, err := os.ReadDir(from)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return nil
		}
		return err
	}
	if err := os.MkdirAll(to, 0o755); err != nil {
		return err
	}
	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		if err := copyFile(filepath.Join(from, e.Name()), filepath.Join(to, e.Name())); err != nil {
			return err
		}
	}
	return nil
}
