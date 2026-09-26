//go:build !windows

package main

import (
	"os"
	"os/exec"
	"syscall"
)

const javaExe = "java"

// detach starts the server in its own session, so it outlives the manager and its terminal.
func detach(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{Setsid: true}
}

// interrupt asks a server to shut down; Paper and Velocity both stop cleanly on SIGTERM.
func interrupt(pid int) error {
	p, err := os.FindProcess(pid)
	if err != nil {
		return err
	}
	return p.Signal(syscall.SIGTERM)
}

func linkDir(target, link string) error {
	return os.Symlink(target, link)
}
