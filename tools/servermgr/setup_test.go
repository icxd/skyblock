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
		n.linkData([]*Server{hub, dungeon}, progress)
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
	// No storage/ in the checkout: nothing said, nothing made. Once it's there, every server gets it.
	if _, err := os.Lstat(filepath.Join(plugin(hub), "storage")); err == nil {
		t.Error("hub01 got a link to a storage/ the data doesn't have")
	}
	if err := os.MkdirAll(filepath.Join(data, "storage"), 0o755); err != nil {
		t.Fatal(err)
	}
	n.linkData([]*Server{hub, dungeon}, progress)
	if len(messages) > 0 {
		t.Fatalf("unexpected messages: %v", messages)
	}
	for _, s := range []*Server{hub, dungeon} {
		if !samePath(filepath.Join(plugin(s), "storage"), filepath.Join(data, "storage")) {
			t.Errorf("%s: storage not linked", s.Name)
		}
	}

	// A folder of the server's own isn't replaced.
	other := &Server{Name: "hub02", Type: "LOBBY"}
	own := filepath.Join(plugin(other), "items")
	if err := os.MkdirAll(own, 0o755); err != nil {
		t.Fatal(err)
	}
	messages = nil
	n.linkData([]*Server{other}, progress)
	if len(messages) != 1 || !strings.Contains(messages[0], "isn't a link") {
		t.Errorf("want one warning about hub02's own items folder, got %v", messages)
	}

	// No items/ in the data checkout yet: said once, nothing made.
	n.DungeonData = filepath.Join(root, "empty")
	messages = nil
	fresh := &Server{Name: "hub03", Type: "LOBBY"}
	n.linkData([]*Server{fresh}, progress)
	if len(messages) != 1 || !strings.Contains(messages[0], "no items/") {
		t.Errorf("want one message about the missing items/, got %v", messages)
	}
	if _, err := os.Lstat(filepath.Join(plugin(fresh), "items")); err == nil {
		t.Error("hub03 got a link to nothing")
	}
}

// What's missing from the data checkout is the same for every server, so deploy says it once.
func TestDeploySaysOnceWhatDataIsMissing(t *testing.T) {
	root := t.TempDir()
	repo := filepath.Join(root, "skyblock")
	jar := filepath.Join(repo, "paper", "target", "skyblock-dungeons-1.0.jar")
	if err := os.MkdirAll(filepath.Dir(jar), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(jar, []byte("jar"), 0o644); err != nil {
		t.Fatal(err)
	}
	n := &Network{Dir: filepath.Join(root, "network"), Repo: repo, Servers: []*Server{
		{Name: "hub01", Type: "LOBBY"}, {Name: "hub02", Type: "LOBBY"},
		{Name: "dungeon01", Type: "DUNGEONS"}, {Name: "dungeon02", Type: "DUNGEONS"},
	}}
	count := func(dungeonData, text string) int {
		n.DungeonData = dungeonData
		var messages []string
		if err := n.deploy(DeployOptions{}, func(m string) { messages = append(messages, m) }); err != nil {
			t.Fatal(err)
		}
		times := 0
		for _, m := range messages {
			if strings.Contains(m, text) {
				times++
			}
		}
		return times
	}

	if got := count("", "No data folder configured"); got != 1 {
		t.Errorf("with no data folder, want the notice once per deploy, got it %d times", got)
	}
	empty := filepath.Join(root, "empty")
	if err := os.MkdirAll(empty, 0o755); err != nil {
		t.Fatal(err)
	}
	if got := count(empty, "no items/"); got != 1 {
		t.Errorf("with no items/ in the data folder, want that said once per deploy, got it %d times", got)
	}
	if got := count(empty, "no rooms/"); got != 1 {
		t.Errorf("with no rooms/ in the data folder, want that said once per deploy, got it %d times", got)
	}

	// Once the checkout has them, the same deploy links them into every server.
	for _, d := range []string{"items", "rooms"} {
		if err := os.MkdirAll(filepath.Join(empty, d), 0o755); err != nil {
			t.Fatal(err)
		}
	}
	if got := count(empty, "has no"); got != 0 {
		t.Errorf("the data folder has everything now, but deploy still said it lacks something %d times", got)
	}
	for _, s := range n.Servers {
		if !samePath(filepath.Join(n.serverDir(s), "plugins", "dungeons", "items"), filepath.Join(empty, "items")) {
			t.Errorf("%s: deploy didn't link items", s.Name)
		}
	}
}

// On Windows without developer mode, linkDir makes junctions, which have to count as links too.
func TestLinkDataJunction(t *testing.T) {
	root := t.TempDir()
	data := filepath.Join(root, "data")
	moved := filepath.Join(root, "moved", "items")
	for _, d := range []string{filepath.Join(data, "items"), moved} {
		if err := os.MkdirAll(d, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	n := &Network{Dir: filepath.Join(root, "network"), DungeonData: data}
	items := func(s *Server) string { return filepath.Join(n.serverDir(s), "plugins", "dungeons", "items") }
	hub := &Server{Name: "hub01", Type: "LOBBY"}
	old := &Server{Name: "hub02", Type: "LOBBY"}
	for s, dest := range map[*Server]string{hub: filepath.Join(data, "items"), old: moved} {
		if err := os.MkdirAll(filepath.Dir(items(s)), 0o755); err != nil {
			t.Fatal(err)
		}
		makeJunction(t, dest, items(s))
	}
	if err := os.RemoveAll(filepath.Dir(moved)); err != nil { // hub02's junction now leads nowhere
		t.Fatal(err)
	}
	var messages []string
	progress := func(m string) { messages = append(messages, m) }

	for i := 0; i < 2; i++ {
		n.linkData([]*Server{hub}, progress)
	}
	if len(messages) > 0 {
		t.Fatalf("hub01's junction to the data isn't taken as a link to it: %v", messages)
	}
	n.linkData([]*Server{old}, progress)
	if len(messages) != 1 || !strings.Contains(messages[0], "relinked") {
		t.Errorf("want one message about relinking hub02, got %v", messages)
	}
	for _, s := range []*Server{hub, old} {
		if !samePath(items(s), filepath.Join(data, "items")) {
			t.Errorf("%s: items not linked", s.Name)
		}
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
	n.linkData([]*Server{hub}, progress)
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
		n.linkData([]*Server{s}, progress)
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
		n.linkData([]*Server{s}, progress)
		if got, err := os.Readlink(items(s)); err != nil || got != dest {
			t.Errorf("%s: its own link to %s became %q (%v)", s.Name, dest, got, err)
		}
		if len(messages) != 1 || !strings.Contains(messages[0], "isn't a link") {
			t.Errorf("%s: want one warning about its own link, got %v", s.Name, messages)
		}
	}
}
