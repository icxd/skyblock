package main

import (
	"bytes"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"regexp"
	"strings"
	"time"
)

// RCON is a Source RCON connection (Paper's rcon, and the proxy plugin's remote console).
type RCON struct {
	conn net.Conn
	id   int32
}

const (
	rconAuth    = 3
	rconCommand = 2
)

func dialRCON(addr, password string, timeout time.Duration) (*RCON, error) {
	conn, err := net.DialTimeout("tcp", addr, timeout)
	if err != nil {
		return nil, err
	}
	c := &RCON{conn: conn}
	id, err := c.send(rconAuth, password)
	if err != nil {
		conn.Close()
		return nil, err
	}
	// Paper sends an empty response before the auth result; the auth result has our id or -1.
	for {
		rid, typ, _, err := c.read(timeout)
		if err != nil {
			conn.Close()
			return nil, err
		}
		if rid == -1 {
			conn.Close()
			return nil, errors.New("wrong RCON password")
		}
		if rid == id && typ == 2 {
			return c, nil
		}
	}
}

func (c *RCON) Close() error { return c.conn.Close() }

// Run sends a command and returns its output, without colour codes.
func (c *RCON) Run(command string) (string, error) {
	id, err := c.send(rconCommand, command)
	if err != nil {
		return "", err
	}
	var out strings.Builder
	timeout := 10 * time.Second
	for {
		rid, _, body, err := c.read(timeout)
		if err != nil {
			if out.Len() > 0 {
				break // no more fragments
			}
			return "", err
		}
		if rid == id {
			out.WriteString(body)
			if len(body) < 4000 {
				break
			}
			// A full packet: long output may continue in the next one.
			timeout = 150 * time.Millisecond
		}
	}
	return stripColors(out.String()), nil
}

func (c *RCON) send(typ int32, body string) (int32, error) {
	c.id++
	var buf bytes.Buffer
	binary.Write(&buf, binary.LittleEndian, int32(len(body)+10))
	binary.Write(&buf, binary.LittleEndian, c.id)
	binary.Write(&buf, binary.LittleEndian, typ)
	buf.WriteString(body)
	buf.Write([]byte{0, 0})
	c.conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	_, err := c.conn.Write(buf.Bytes())
	return c.id, err
}

func (c *RCON) read(timeout time.Duration) (int32, int32, string, error) {
	c.conn.SetReadDeadline(time.Now().Add(timeout))
	var size int32
	if err := binary.Read(c.conn, binary.LittleEndian, &size); err != nil {
		return 0, 0, "", err
	}
	if size < 10 || size > 1<<20 {
		return 0, 0, "", fmt.Errorf("bad RCON packet size %d", size)
	}
	data := make([]byte, size)
	if _, err := io.ReadFull(c.conn, data); err != nil {
		return 0, 0, "", err
	}
	id := int32(binary.LittleEndian.Uint32(data[0:4]))
	typ := int32(binary.LittleEndian.Uint32(data[4:8]))
	return id, typ, string(data[8 : len(data)-2]), nil
}

var colorCodes = regexp.MustCompile(`§[0-9a-fk-orx]`)

func stripColors(s string) string { return colorCodes.ReplaceAllString(s, "") }
