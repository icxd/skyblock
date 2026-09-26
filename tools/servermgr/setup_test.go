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

// The README's quick start runs init from the repository with --data ../skyblock-dungeon-data, and
// older network.json files keep that relative path.
func TestLinkDataRelativePath(t *testing.T) {
	root := t.TempDir()
	repo := filepath.Join(root, "skyblock")
	data := filepath.Join(root, "skyblock-dungeon-data")
	if err := os.MkdirAll(filepath.Join(data, "items"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(data, "items", "items.json"), []byte("[]"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(repo, 0o755); err != nil {
		t.Fatal(err)
	}
	t.Chdir(repo)
	n := &Network{Dir: filepath.Join(repo, "network"), DungeonData: "../skyblock-dungeon-data"}
	var messages []string
	progress := func(m string) { messages = append(messages, m) }
	items := func(s *Server) string { return filepath.Join(n.serverDir(s), "plugins", "dungeons", "items") }
	reachable := func(s *Server) bool {
		_, err := os.Stat(filepath.Join(items(s), "items.json"))
		return err == nil
	}

	hub := &Server{Name: "hub01", Type: "LOBBY"}
	n.linkData(hub, progress)
	if !reachable(hub) {
		t.Errorf("hub01: items.json isn't there through the link (messages %v)", messages)
	}

	// Links left leading nowhere are servermgr's to replace: one made from the relative path as it
	// used to be, and one to a data checkout that has since moved.
	stale := map[*Server]string{
		{Name: "hub02", Type: "LOBBY"}: filepath.Join(n.DungeonData, "items"),
		{Name: "hub03", Type: "LOBBY"}: filepath.Join(root, "moved-away", "items"),
	}
	for s, dest := range stale {
		if err := os.MkdirAll(filepath.Dir(items(s)), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.Symlink(dest, items(s)); err != nil {
			t.Fatal(err)
		}
		messages = nil
		n.linkData(s, progress)
		if !reachable(s) {
			t.Errorf("%s: the link to %s wasn't replaced (messages %v)", s.Name, dest, messages)
		}
		if len(messages) != 1 || !strings.Contains(messages[0], "relinked") {
			t.Errorf("%s: want one message about relinking, got %v", s.Name, messages)
		}
	}

	// Links of the user's own stay, even one that leads nowhere.
	elsewhere := filepath.Join(root, "my-items")
	if err := os.MkdirAll(elsewhere, 0o755); err != nil {
		t.Fatal(err)
	}
	own := map[*Server]string{
		{Name: "hub04", Type: "LOBBY"}: elsewhere,
		{Name: "hub05", Type: "LOBBY"}: filepath.Join(root, "unplugged-drive"),
	}
	for s, dest := range own {
		if err := os.MkdirAll(filepath.Dir(items(s)), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.Symlink(dest, items(s)); err != nil {
			t.Fatal(err)
		}
		messages = nil
		n.linkData(s, progress)
		if got, err := os.Readlink(items(s)); err != nil || got != dest {
			t.Errorf("%s: its own link to %s became %q (%v)", s.Name, dest, got, err)
		}
		if len(messages) != 1 || !strings.Contains(messages[0], "isn't a link") {
			t.Errorf("%s: want one warning about its own link, got %v", s.Name, messages)
		}
	}
}
