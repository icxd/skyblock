//go:build windows

package main

import "testing"

// makeJunction makes a directory junction, which linkDir falls back to without developer mode.
func makeJunction(t *testing.T, target, link string) {
	t.Helper()
	if err := junction(target, link); err != nil {
		t.Fatal(err)
	}
}
