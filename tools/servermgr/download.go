package main

import (
	"crypto/sha256"
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"hash"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"time"
)

const userAgent = "icxd/skyblock-servermgr (github.com/icxd/skyblock)"

var httpClient = &http.Client{Timeout: 10 * time.Minute}

// Artifact is something to download: a server jar or a plugin.
type Artifact struct {
	Name     string // file name
	URL      string
	Build    int    // PaperMC build number, 0 for plugins
	Version  string // plugin version
	Checksum string // hex
	hashNew  func() hash.Hash
}

func getJSON(u string, into any) error {
	req, err := http.NewRequest("GET", u, nil)
	if err != nil {
		return err
	}
	req.Header.Set("User-Agent", userAgent)
	resp, err := httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return fmt.Errorf("%s: %s", u, resp.Status)
	}
	return json.NewDecoder(resp.Body).Decode(into)
}

// latestPaperMC is the newest build of a PaperMC project (paper, velocity) for a version.
func latestPaperMC(project, version string) (*Artifact, error) {
	var build struct {
		ID        int `json:"id"`
		Downloads map[string]struct {
			Name      string            `json:"name"`
			URL       string            `json:"url"`
			Checksums map[string]string `json:"checksums"`
		} `json:"downloads"`
	}
	u := fmt.Sprintf("https://fill.papermc.io/v3/projects/%s/versions/%s/builds/latest", project, url.PathEscape(version))
	if err := getJSON(u, &build); err != nil {
		return nil, fmt.Errorf("looking up %s %s: %w", project, version, err)
	}
	d, ok := build.Downloads["server:default"]
	if !ok {
		return nil, fmt.Errorf("%s %s build %d has no server download", project, version, build.ID)
	}
	return &Artifact{Name: d.Name, URL: d.URL, Build: build.ID, Checksum: d.Checksums["sha256"], hashNew: sha256.New}, nil
}

// latestModrinth is the newest release of a Modrinth project for Paper on a Minecraft version.
func latestModrinth(slug, gameVersion string) (*Artifact, error) {
	var versions []struct {
		VersionNumber string `json:"version_number"`
		VersionType   string `json:"version_type"`
		Files         []struct {
			URL      string            `json:"url"`
			Filename string            `json:"filename"`
			Primary  bool              `json:"primary"`
			Hashes   map[string]string `json:"hashes"`
		} `json:"files"`
	}
	q := url.Values{}
	q.Set("loaders", `["paper"]`)
	q.Set("game_versions", fmt.Sprintf(`["%s"]`, gameVersion))
	if err := getJSON("https://api.modrinth.com/v2/project/"+slug+"/version?"+q.Encode(), &versions); err != nil {
		return nil, fmt.Errorf("looking up %s: %w", slug, err)
	}
	for pass := 0; pass < 2; pass++ {
		for _, v := range versions {
			// Releases first; a beta only if there's no release for this Minecraft version yet.
			if pass == 0 && v.VersionType != "release" {
				continue
			}
			for _, f := range v.Files {
				if f.Primary || len(v.Files) == 1 {
					return &Artifact{Name: f.Filename, URL: f.URL, Version: v.VersionNumber, Checksum: f.Hashes["sha512"], hashNew: sha512.New}, nil
				}
			}
		}
	}
	return nil, fmt.Errorf("no %s release for Paper %s on Modrinth", slug, gameVersion)
}

// fetch downloads an artifact into the cache (once) and returns its path.
func (n *Network) fetch(a *Artifact, progress func(string)) (string, error) {
	if err := os.MkdirAll(n.cacheDir(), 0o755); err != nil {
		return "", err
	}
	dest := filepath.Join(n.cacheDir(), a.Name)
	if _, err := os.Stat(dest); err == nil {
		return dest, nil
	}
	progress(fmt.Sprintf("Downloading %s", a.Name))
	req, err := http.NewRequest("GET", a.URL, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("User-Agent", userAgent)
	resp, err := httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return "", fmt.Errorf("downloading %s: %s", a.Name, resp.Status)
	}
	tmp := dest + ".part"
	f, err := os.Create(tmp)
	if err != nil {
		return "", err
	}
	var w io.Writer = f
	var h hash.Hash
	if a.Checksum != "" && a.hashNew != nil {
		h = a.hashNew()
		w = io.MultiWriter(f, h)
	}
	_, err = io.Copy(w, resp.Body)
	if cerr := f.Close(); err == nil {
		err = cerr
	}
	if err != nil {
		os.Remove(tmp)
		return "", fmt.Errorf("downloading %s: %w", a.Name, err)
	}
	if h != nil && hex.EncodeToString(h.Sum(nil)) != a.Checksum {
		os.Remove(tmp)
		return "", fmt.Errorf("%s: checksum doesn't match, not using it", a.Name)
	}
	return dest, os.Rename(tmp, dest)
}

func copyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return err
	}
	tmp := dst + ".tmp"
	out, err := os.Create(tmp)
	if err != nil {
		return err
	}
	if _, err := io.Copy(out, in); err != nil {
		out.Close()
		os.Remove(tmp)
		return err
	}
	if err := out.Close(); err != nil {
		return err
	}
	return os.Rename(tmp, dst)
}
