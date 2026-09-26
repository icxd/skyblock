#!/usr/bin/env node
'use strict'
// Decodes Replay Mod recordings into something readable. See README.md.
const fs = require('fs')
const path = require('path')
const nbt = require('prismarine-nbt')
const recording = require('./lib/recording')
const { decode, clock } = require('./lib/timeline')
const { renderMaps } = require('./lib/maps')
const { setup } = require('./lib/setup')
const { text, plain } = require('./lib/text')

const DATA = path.join(__dirname, 'data')
const USAGE = `Usage:
  replay.js setup <server.jar> [--java <path>]      tables for newer versions, from a vanilla server jar
  replay.js decode <recording.mcpr> [--out <dir>]   timeline, sidebar, tab list and menus as text
  replay.js maps <recording.mcpr> <seconds>... [--map <id>] [--out <dir>]
                                                     maps as PNGs at those moments
  replay.js item <recording.mcpr> <slot> <from s> <to s>
                                                     name, lore and map id of items put in an inventory slot
  replay.js raw <recording.mcpr> <from s> <to s> <packet>...
                                                     decoded packets of those types, as JSON
  replay.js scan <recording.mcpr>                   packet counts and decoding errors`

function option (args, name, fallback) {
  const i = args.indexOf(name)
  if (i === -1) return fallback
  const value = args[i + 1]
  args.splice(i, 2)
  return value
}

function readData (file) {
  const p = path.join(DATA, file)
  return fs.existsSync(p) ? JSON.parse(fs.readFileSync(p)) : null
}

/** Opens a recording and its decoders, saying what it is and warning about what might be off. */
function load (file) {
  const rec = recording.open(file)
  const dec = recording.decoders(rec.meta)
  const m = rec.meta
  console.error(`${path.basename(file)}: Minecraft ${m.mcversion} (protocol ${m.protocol}), ${clock(m.duration || 0)} long, ${m.serverName || '?'}; decoding as ${dec.version}${dec.notes ? ' (' + dec.notes + ')' : ''}`)
  const version = readData('version.json')
  const generated = version && version.version === m.mcversion
  if (!generated && dec.version !== m.mcversion) {
    console.error(`  no tables for ${m.mcversion}: run \`replay.js setup\` with a ${m.mcversion} server jar for correct item ids and map colours`)
  }
  // The packet ids must be the same as the decoding version's, or everything is off.
  const report = generated && readData('packets.json')
  if (report) {
    for (const state of ['configuration', 'play']) {
      const theirs = Object.keys(report[state].clientbound).length
      const ours = Object.keys(dec.data.protocol[state].toClient.types.packet[1][0].type[1].mappings).length
      if (theirs !== ours) console.error(`  WARNING: ${m.mcversion} has ${theirs} ${state} packets, ${dec.version} has ${ours}; decoding will be wrong`)
    }
  }
  let items = generated && readData('items.json')
  if (!items) {
    items = {}
    for (const item of dec.data.itemsArray) items[item.id] = item.name
  }
  return { rec, dec, items, colors: generated ? readData('map-colors.json') : null }
}

function outDir (args, file, kind) {
  return option(args, '--out', path.join(__dirname, 'out', path.basename(file).replace(/\.t?mcpr$/, '') + (kind ? '-' + kind : '')))
}

function main (argv) {
  const [command, ...args] = argv
  switch (command) {
    case 'setup': {
      const java = option(args, '--java', process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java')
      if (!args[0]) break
      setup(args[0], DATA, java)
      return
    }
    case 'decode': {
      const dir = outDir(args, args[0])
      if (!args[0]) break
      const { rec, dec, items } = load(args[0])
      const stats = decode(rec, dec, items, dir)
      console.error(`${stats.packets} packets (${stats.errors} undecodable); ${stats.timeline} timeline entries, ${stats.sidebars} sidebars, ${stats.tabs} tab lists -> ${dir}`)
      return
    }
    case 'maps': {
      const only = option(args, '--map', null)
      const dir = outDir(args, args[0], 'maps')
      if (args.length < 2) break
      const { rec, dec, colors } = load(args[0])
      if (!colors) throw new Error('no map colours for this version yet: run `replay.js setup` first')
      const files = renderMaps(rec, dec, colors, dir, args.slice(1).map(s => Number(s) * 1000), only ? [Number(only)] : null)
      console.error(`${files.length} images -> ${dir}`)
      return
    }
    case 'item': {
      if (args.length < 4) break
      const { rec, dec } = load(args[0])
      const [slot, from, to] = args.slice(1).map(Number)
      for (const p of recording.packets(rec, dec)) {
        if (p.error || p.t < from * 1000 || p.t > to * 1000) continue
        let item = null
        if (p.name === 'set_slot' && p.params.slot === slot && (p.params.windowId === 0 || p.params.windowId === -2)) item = p.params.item
        if (p.name === 'window_items' && p.params.windowId === 0) item = p.params.items[slot]
        if (!item || !item.itemCount) continue
        for (const c of item.components || []) {
          if (c.type === 'custom_name' || c.type === 'item_name') console.log(clock(p.t), 'name:', plain(text(c.data)))
          if (c.type === 'map_id') console.log(clock(p.t), 'map id:', JSON.stringify(c.data))
          if (c.type === 'lore') console.log(c.data.map(l => '    ' + plain(text(l))).join('\n'))
        }
      }
      return
    }
    case 'raw': {
      if (args.length < 4) break
      const { rec, dec } = load(args[0])
      const [from, to] = args.slice(1, 3).map(Number)
      const names = args.slice(3)
      const simplify = v => (v && typeof v === 'object' && typeof v.type === 'string' && v.value !== undefined) ? nbt.simplify(v) : v
      for (const p of recording.packets(rec, dec)) {
        if (p.error || p.t < from * 1000 || p.t > to * 1000 || !names.includes(p.name)) continue
        const params = {}
        for (const [k, v] of Object.entries(p.params)) params[k] = simplify(v)
        console.log(clock(p.t), p.name, JSON.stringify(params))
      }
      return
    }
    case 'scan': {
      if (!args[0]) break
      const { rec, dec } = load(args[0])
      const counts = {}
      const errors = {}
      for (const p of recording.packets(rec, dec)) {
        const key = p.state + ':' + (p.error ? '0x' + p.id.toString(16) : p.name)
        counts[key] = (counts[key] || 0) + 1
        if (p.error) errors[key + ' ' + p.error.slice(0, 60)] = (errors[key + ' ' + p.error.slice(0, 60)] || 0) + 1
        else if (p.leftover) errors[key + ' (bytes left over)'] = (errors[key + ' (bytes left over)'] || 0) + 1
      }
      console.log(Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${v}\t${k}`).join('\n'))
      console.log('\nNot fully decoded:')
      console.log(Object.entries(errors).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${v}\t${k}`).join('\n') || '  none')
      return
    }
  }
  console.error(USAGE)
  process.exitCode = 1
}

try {
  main(process.argv.slice(2))
} catch (e) {
  console.error(e.message)
  process.exitCode = 1
}
