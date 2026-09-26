//go:build !windows

package main

import "testing"

// makeJunction: junctions are Windows only. Here linkDir makes symlinks, which the other tests cover.
func makeJunction(t *testing.T, target, link string) {
	t.Helper()
	t.Skip("junctions are Windows only")
}
