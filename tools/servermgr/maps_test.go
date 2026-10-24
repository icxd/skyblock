package main

import (
	"bytes"
	"compress/gzip"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// capture writes a /dscan world capture into data/worlds/folder: its capture.json and a region file.
func capture(t *testing.T, data, folder, areas, to string) string {
	t.Helper()
	dir := filepath.Join(data, "worlds", folder)
	for _, sub := range []string{"region", "entities"} {
		if err := os.MkdirAll(filepath.Join(dir, sub), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(filepath.Join(dir, sub, "r.0.0.mca"), []byte(folder+sub), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	json := `{"areas": ` + areas + `, "to": "` + to + `"}`
	if err := os.WriteFile(filepath.Join(dir, "capture.json"), []byte(json), 0o644); err != nil {
		t.Fatal(err)
	}
	return dir
}

// The user's first captures keep Hypixel's made-up codes in their area names.
func TestFindCapture(t *testing.T) {
	data := t.TempDir()
	hub := capture(t, data, "1_village_u", `["Village§u", "Mining Di§ustrict"]`, "2026-10-24T10:54:06Z")
	capture(t, data, "1_your_isla_tnd", `["Your Isla§tnd"]`, "2026-10-24T10:33:26Z")
	newer := capture(t, data, "2_dungeon_hub", `["Dungeon Hub"]`, "2026-11-01T00:00:00Z")
	capture(t, data, "1_dungeon_h_tub", `["Dungeon H§tub"]`, "2026-10-24T10:50:36Z")
	if got, _ := findCapture(data, "Village"); got != hub {
		t.Errorf("Village: %q", got)
	}
	if got, _ := findCapture(data, "Dungeon Hub"); got != newer {
		t.Errorf("Dungeon Hub: %q, want the newer one", got)
	}
	if got, _ := findCapture(data, "The End"); got != "" {
		t.Errorf("The End: %q", got)
	}
	if got, err := findCapture(t.TempDir(), "Village"); got != "" || err != nil {
		t.Errorf("no worlds/: %q %v", got, err)
	}
}

func TestInstallMap(t *testing.T) {
	root := t.TempDir()
	data := filepath.Join(root, "data")
	capture(t, data, "1_village_u", `["Village§u"]`, "2026-10-24T10:54:06Z")
	n := &Network{Dir: filepath.Join(root, "network"), DungeonData: data}
	hub := &Server{Name: "hub01", Type: "LOBBY", FirstStartDone: true}
	n.Servers = []*Server{hub}
	if err := os.MkdirAll(filepath.Join(n.serverDir(hub), "world", "region"), 0o755); err != nil {
		t.Fatal(err)
	}
	var messages []string
	if err := n.installMap(hub, "hub", func(m string) { messages = append(messages, m) }); err != nil {
		t.Fatal(err)
	}
	region, err := os.ReadFile(filepath.Join(n.serverDir(hub), "world", "dimensions", "minecraft", "overworld", "region", "r.0.0.mca"))
	if err != nil || string(region) != "1_village_uregion" {
		t.Fatalf("region file: %q %v", region, err)
	}
	if _, err := os.Stat(filepath.Join(n.serverDir(hub), "world", "dimensions", "minecraft", "overworld", "entities", "r.0.0.mca")); err != nil {
		t.Error("entities not copied")
	}
	if _, err := os.Stat(filepath.Join(n.serverDir(hub), "world", "dimensions", "minecraft", "overworld", "data", "minecraft", "world_gen_settings.dat")); err != nil {
		t.Error("no generator settings: Paper 26 won't start the world")
	}
	kept, _ := filepath.Glob(filepath.Join(n.serverDir(hub), "world-before-*"))
	if len(kept) != 1 {
		t.Errorf("the old world wasn't kept: %v", kept)
	}
	props, _ := os.ReadFile(filepath.Join(n.serverDir(hub), "server.properties"))
	if !strings.Contains(string(props), `generator-settings={"biome"\:"minecraft\:the_void"`) || !strings.Contains(string(props), `level-type=minecraft\:flat`) {
		t.Errorf("server.properties:\n%s", props)
	}
	config, _ := os.ReadFile(filepath.Join(n.serverDir(hub), "plugins", "dungeons", "config.yml"))
	if !strings.Contains(string(config), "map: hub") {
		t.Errorf("config.yml:\n%s", config)
	}
	if hub.Map != "hub" || hub.FirstStartDone {
		t.Errorf("server: map %q, first start done %v", hub.Map, hub.FirstStartDone)
	}
	if err := n.installMap(hub, "nowhere", func(string) {}); err == nil {
		t.Error("an unknown map went in")
	}
	if err := n.installMap(hub, "none", func(string) {}); err != nil || hub.Map != "" {
		t.Errorf("taking the map off: %v, map %q", err, hub.Map)
	}
}

// The generator settings as a 26.2 server reads them: gzipped NBT, its overworld flat and empty.
func TestVoidWorldGenSettings(t *testing.T) {
	zr, err := gzip.NewReader(bytes.NewReader(voidWorldGenSettings()))
	if err != nil {
		t.Fatal(err)
	}
	raw, err := io.ReadAll(zr)
	if err != nil {
		t.Fatal(err)
	}
	if raw[0] != 10 || raw[len(raw)-1] != 0 {
		t.Fatalf("not one compound: % x ... % x", raw[:3], raw[len(raw)-1])
	}
	for _, want := range []string{"DataVersion", "minecraft:flat", "minecraft:air", "minecraft:the_void", "minecraft:the_end", "seed", "bonus_chest"} {
		if !bytes.Contains(raw, []byte(want)) {
			t.Errorf("no %q in it", want)
		}
	}
}
