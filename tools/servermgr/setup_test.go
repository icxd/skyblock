package main

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestLinkData(t *testing.T) {
	root := t.TempDir()
	data := filepath.Join(root, "data")
	for _, d := range []string{"items", "rooms"} {
		if err := os.MkdirAll(filepath.Join(data, d), 0o755); err != nil {
			t.Fatal(err)
		}
	}
	n := &Network{Dir: filepath.Join(root, "network"), DungeonData: data}
	hub := &Server{Name: "hub01", Type: "LOBBY"}
	dungeon := &Server{Name: "dungeon01", Type: "DUNGEONS"}
	var messages []string
	progress := func(m string) { messages = append(messages, m) }

	for i := 0; i < 2; i++ { // the second time finds its links already there
		n.linkData(hub, progress)
		n.linkData(dungeon, progress)
	}
	if len(messages) > 0 {
		t.Fatalf("unexpected messages: %v", messages)
	}
	plugin := func(s *Server) string { return filepath.Join(n.serverDir(s), "plugins", "dungeons") }
	for _, s := range []*Server{hub, dungeon} {
		if !samePath(filepath.Join(plugin(s), "items"), filepath.Join(data, "items")) {
			t.Errorf("%s: items not linked", s.Name)
		}
	}
	if !samePath(filepath.Join(plugin(dungeon), "dungeon-rooms", "rooms"), filepath.Join(data, "rooms")) {
		t.Error("dungeon01: rooms not linked")
	}
	if _, err := os.Lstat(filepath.Join(plugin(hub), "dungeon-rooms")); err == nil {
		t.Error("hub01 got the dungeon rooms")
	}

	// A folder of the server's own isn't replaced.
	other := &Server{Name: "hub02", Type: "LOBBY"}
	own := filepath.Join(plugin(other), "items")
	if err := os.MkdirAll(own, 0o755); err != nil {
		t.Fatal(err)
	}
	messages = nil
	n.linkData(other, progress)
	if len(messages) != 1 || !strings.Contains(messages[0], "isn't a link") {
		t.Errorf("want one warning about hub02's own items folder, got %v", messages)
	}

	// No items/ in the data checkout yet: said once, nothing made.
	n.DungeonData = filepath.Join(root, "empty")
	messages = nil
	fresh := &Server{Name: "hub03", Type: "LOBBY"}
	n.linkData(fresh, progress)
	if len(messages) != 1 || !strings.Contains(messages[0], "no items/") {
		t.Errorf("want one message about the missing items/, got %v", messages)
	}
	if _, err := os.Lstat(filepath.Join(plugin(fresh), "items")); err == nil {
		t.Error("hub03 got a link to nothing")
	}
}
