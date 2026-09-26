package main

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"sync"
	"time"
)

// javaHome is the JDK folder of a java executable (for Maven's JAVA_HOME).
func javaHome(java string) string {
	path, err := exec.LookPath(java)
	if err != nil {
		path = java
	}
	if resolved, err := filepath.EvalSymlinks(path); err == nil {
		path = resolved
	}
	return filepath.Dir(filepath.Dir(path))
}

// build runs the repository's Maven build with the network's JDK.
func (n *Network) build(output func(string)) error {
	if n.Repo == "" {
		return errors.New("no repository configured (\"repo\" in network.json)")
	}
	mvn := "mvn"
	if runtime.GOOS == "windows" {
		mvn = "mvn.cmd"
	}
	cmd := exec.Command(mvn, "-q", "package", "-DskipTests")
	cmd.Dir = n.Repo
	// Maven builds with JAVA_HOME, not the java on PATH.
	cmd.Env = append(os.Environ(), "JAVA_HOME="+javaHome(n.Java))
	pr, pw := io.Pipe()
	cmd.Stdout = pw
	cmd.Stderr = pw
	output("Building the plugins (mvn package)")
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("running %s: %w (is Maven installed and on PATH?)", mvn, err)
	}
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		sc := bufio.NewScanner(pr)
		for sc.Scan() {
			if line := sc.Text(); line != "" {
				output("  " + line)
			}
		}
	}()
	err := cmd.Wait()
	pw.Close()
	wg.Wait()
	if err != nil {
		return fmt.Errorf("the build failed: %w", err)
	}
	return nil
}

// DeployOptions: build first, and restart what's running afterwards.
type DeployOptions struct {
	Build, Restart bool
}

// deploy puts the freshly built plugins on every server.
func (n *Network) deploy(o DeployOptions, output func(string)) error {
	if o.Build {
		if err := n.build(output); err != nil {
			return err
		}
	}
	paper := n.builtJar("paper", "skyblock-dungeons")
	proxy := n.builtJar("proxy", "skyblock-proxy")
	if paper == "" && proxy == "" {
		return errors.New("nothing built yet; deploy with the build on")
	}
	var restart []string
	if paper != "" {
		for _, s := range n.Servers {
			if err := installJar(paper, filepath.Join(n.serverDir(s), "plugins"), "skyblock-dungeons-"); err != nil {
				output(fmt.Sprintf("! %s: %v", s.Name, err))
				continue
			}
			if n.state(s.Name).Alive {
				restart = append(restart, s.Name)
			}
		}
		output(fmt.Sprintf("Installed %s on %d servers", filepath.Base(paper), len(n.Servers)))
	}
	if proxy != "" {
		if err := installJar(proxy, filepath.Join(n.proxyDir(), "plugins"), "skyblock-proxy-"); err != nil {
			output("! proxy: " + err.Error())
		} else {
			output("Installed " + filepath.Base(proxy) + " on the proxy")
			if n.state(proxyDirName).Alive {
				// Last, so players aren't dropped before their servers are back.
				restart = append(restart, proxyDirName)
			}
		}
	}
	if !o.Restart {
		if len(restart) > 0 {
			output(fmt.Sprintf("Restart %d running servers to load it", len(restart)))
		}
		return nil
	}
	for _, name := range restart {
		if err := n.restart(name, output); err != nil {
			output(fmt.Sprintf("! %s: %v", name, err))
		}
	}
	return nil
}

func (n *Network) restart(name string, output func(string)) error {
	output("Restarting " + name)
	if err := n.stop(name, 90*time.Second); err != nil {
		return err
	}
	return n.startAndWait(name, output)
}

// startAndWait starts a server and waits until it answers, running first-start setup.
func (n *Network) startAndWait(name string, output func(string)) error {
	if err := n.start(name); err != nil {
		return err
	}
	output("Starting " + name)
	if err := n.waitReady(name, 3*time.Minute); err != nil {
		return err
	}
	if s := n.server(name); s != nil {
		if err := n.firstStart(s); err != nil {
			output("! First-start setup on " + name + ": " + err.Error())
		}
	}
	output(name + " is up")
	return nil
}

// update gets the newest Paper, Velocity and plugin builds. Running servers get theirs when
// they next start.
func (n *Network) update(output func(string)) error {
	output("Checking for newer builds")
	paper, err := latestPaperMC("paper", n.PaperVersion)
	if err != nil {
		return err
	}
	for _, s := range n.Servers {
		if paper.Build <= s.Build {
			continue
		}
		if err := n.replaceJar(s.Name, n.serverDir(s), "server.jar", paper, output); err != nil {
			output(fmt.Sprintf("! %s: %v", s.Name, err))
			continue
		}
		s.Build = paper.Build
	}
	velocity, err := latestPaperMC("velocity", n.VelocityVersion)
	if err != nil {
		return err
	}
	if velocity.Build > n.Proxy.Build {
		if err := n.replaceJar(proxyDirName, n.proxyDir(), "velocity.jar", velocity, output); err != nil {
			output("! proxy: " + err.Error())
		} else {
			n.Proxy.Build = velocity.Build
		}
	}
	for _, p := range modrinthPlugins {
		a, err := latestModrinth(p[0], n.PaperVersion)
		if err != nil {
			output("! " + err.Error())
			continue
		}
		path, err := n.fetch(a, output)
		if err != nil {
			output("! " + err.Error())
			continue
		}
		for _, s := range n.Servers {
			plugins := filepath.Join(n.serverDir(s), "plugins")
			if _, err := os.Stat(filepath.Join(plugins, a.Name)); err == nil {
				continue
			}
			if n.state(s.Name).Alive {
				output(fmt.Sprintf("%s: stop it to update %s to %s", s.Name, p[0], a.Version))
				continue
			}
			if err := installJar(path, plugins, p[1]); err != nil {
				output(fmt.Sprintf("! %s: %v", s.Name, err))
			} else {
				output(fmt.Sprintf("%s: %s %s", s.Name, p[0], a.Version))
			}
		}
	}
	output(fmt.Sprintf("Up to date: Paper %s build %d, Velocity %s build %d", n.PaperVersion, paper.Build, n.VelocityVersion, velocity.Build))
	return n.save()
}

// replaceJar installs a new server jar; for a running server it waits as <jar>.new until the next start.
func (n *Network) replaceJar(name, dir, jar string, a *Artifact, output func(string)) error {
	path, err := n.fetch(a, output)
	if err != nil {
		return err
	}
	dest := filepath.Join(dir, jar)
	if n.state(name).Alive {
		dest += ".new"
		output(name + ": build " + strconv.Itoa(a.Build) + " is used from the next start")
	} else {
		output(name + ": now on build " + strconv.Itoa(a.Build))
	}
	return copyFile(path, dest)
}
