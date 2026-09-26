'use strict'
// Maps (item frames, the dungeon map, score cards) rendered as they looked at given moments.
const fs = require('fs')
const path = require('path')
const { PNG } = require('pngjs')
const { packets } = require('./recording')

const SIZE = 128
const SCALE = 4

function render (pixels, colors) {
  const png = new PNG({ width: SIZE * SCALE, height: SIZE * SCALE })
  for (let y = 0; y < SIZE * SCALE; y++) {
    for (let x = 0; x < SIZE * SCALE; x++) {
      const id = pixels[Math.floor(y / SCALE) * SIZE + Math.floor(x / SCALE)]
      const argb = colors[id] >>> 0
      const o = (y * SIZE * SCALE + x) * 4
      // Ids 0-3 are transparent.
      png.data[o] = id < 4 ? 34 : (argb >> 16) & 255
      png.data[o + 1] = id < 4 ? 34 : (argb >> 8) & 255
      png.data[o + 2] = id < 4 ? 34 : argb & 255
      png.data[o + 3] = 255
    }
  }
  return PNG.sync.write(png)
}

/** Writes map<id>_<seconds>s.png for every map (or only `onlyIds`) at each of `times` (ms). */
function renderMaps (recording, dec, colors, outDir, times, onlyIds) {
  const maps = {}
  const pending = [...times].sort((a, b) => a - b)
  const written = []
  fs.mkdirSync(outDir, { recursive: true })
  const save = t => {
    for (const [id, pixels] of Object.entries(maps)) {
      if (onlyIds && !onlyIds.includes(Number(id))) continue
      const file = path.join(outDir, `map${id}_${(t / 1000).toFixed(1).replace('.', '_')}s.png`)
      fs.writeFileSync(file, render(pixels, colors))
      written.push(file)
    }
  }
  for (const p of packets(recording, dec)) {
    while (pending.length && p.t > pending[0]) save(pending.shift())
    if (p.error || p.name !== 'map' || !p.params.columns) continue
    const d = p.params
    const pixels = maps[d.itemDamage] ||= new Uint8Array(SIZE * SIZE)
    for (let r = 0; r < d.rows; r++) {
      for (let c = 0; c < d.columns; c++) pixels[(d.y + r) * SIZE + d.x + c] = d.data[r * d.columns + c]
    }
  }
  while (pending.length) save(pending.shift())
  return written
}

module.exports = { renderMaps }
