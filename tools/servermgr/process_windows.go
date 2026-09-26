//go:build windows

package main

import (
	"errors"
	"os"
	"os/exec"
	"syscall"
)

const javaExe = "java.exe"

const (
	createNewProcessGroup = 0x00000200
	detachedProcess       = 0x00000008
)

// detach starts the server without a console window, outside the manager's console, so it keeps
// running when the manager closes.
func detach(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{CreationFlags: createNewProcessGroup | detachedProcess}
}

// interrupt isn't possible for a detached process on Windows; the caller falls back to the
// console command or killing it.
func interrupt(pid int) error {
	return errors.New("no signals on Windows")
}

// linkDir tries a symlink (needs developer mode or admin), then a directory junction (doesn't).
func linkDir(target, link string) error {
	if err := os.Symlink(target, link); err == nil {
		return nil
	}
	out, err := exec.Command("cmd", "/c", "mklink", "/J", link, target).CombinedOutput()
	if err != nil {
		return errors.New(string(out))
	}
	return nil
}
