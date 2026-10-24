'use strict'
// Who stands where in each world a recording visits.
//   npcs.txt   per world: each NPC with its name tag, position, facing, skin and what it holds;
//              the holograms that stand on their own; and the named mobs, counted by name
//   npcs.json  the same, with each NPC's "textures" property (value and signature) to spawn it with
//
// Hypixel's NPCs are players with version 2 UUIDs that the tab list doesn't show (real players
// have version 4), or mobs and mannequins that stand still. Their names float above them on
// invisible armor stands ("The Delivery Man", "RIGHT CLICK"); text displays work the same way.
const { text, plain, clock } = require('./text')

// Equipment slots, by their number in the equipment packet.
const SLOTS = ['mainhand', 'offhand', 'feet', 'legs', 'chest', 'head', 'body', 'saddle']
// A name tag is an entity's when it's in its column, at most this far off...
const COLUMN = 0.35
// ...from just below its feet to this far above them (or when it rides it).
const ABOVE = 3.5
// Anything that went further than this from where it appeared is a mob or a player, not an NPC.
const STILL = 1.5
// Texts kept per name tag (numbers aside), so a counter or a cycling tag doesn't fill the file.
const MAX_TEXTS = 12
// What can't be an NPC's body: what isn't a mob or a player.
const NOT_A_BODY = /^(armor_stand|.*_display|interaction|marker|item|experience_orb|.*item_frame|painting|falling_block|.*arrow|trident|snowball|egg|.*ender_pearl|.*fireball|wither_skull|.*potion|experience_bottle|firework_rocket|fishing_bobber|lightning_bolt|area_effect_cloud|evoker_fangs|llama_spit|shulker_bullet|.*wind_charge|eye_of_ender|tnt|end_crystal|leash_knot|.*boat|.*raft|.*minecart|ominous_item_spawner)$/
// Name tags of SkyBlock's mobs (a level or health), and the damage numbers that pop out of them.
const MOB_TAG = /❤|\bLv ?\d|\[Lv/
const DAMAGE = /^\s*[✧✯]?[\d,.]+[✧✯]?\s*$/
// Holograms gone this soon after they appeared were damage numbers and the like.
const BRIEF = 1500
const CLICK = /^(right |left )?click\b/i
const COMPASS = ['south', 'southwest', 'west', 'northwest', 'north', 'northeast', 'east', 'southeast']

const numbersAside = s => plain(s).replace(/[\d,.]+/g, '#')
const round = v => Math.round(v * 100) / 100
const angle = byte => round(byte * 360 / 256)

/** A profile's skin: its "textures" property, and the texture's URL and model from inside it. */
function skin (properties) {
  const p = (properties || []).find(x => x.name === 'textures')
  if (!p) return null
  const out = { value: p.value, signature: p.signature ?? null }
  try {
    const texture = JSON.parse(Buffer.from(p.value, 'base64').toString('utf8')).textures.SKIN
    out.url = texture.url
    if (texture.metadata && texture.metadata.model === 'slim') out.slim = true
  } catch (e) {
    // Not the usual JSON: the property is still there to spawn it with.
  }
  return out
}

/** Hypixel's NPCs don't have the version 4 UUIDs of real accounts. */
const isNpcUuid = uuid => typeof uuid === 'string' && uuid[14] !== '4'

/**
 * Follows entities through the play packets. `types` names entity type ids (from `setup`);
 * `describe` turns an item stack into { id, count, name, lore } or null.
 */
function track (types, describe) {
  const profiles = {}
  const worlds = []
  let world = null
  let lastT = 0

  const newWorld = (t, how) => {
    if (world) close(t)
    world = { t, end: null, how, server: null, areas: [], live: new Map(), done: [] }
    worlds.push(world)
  }
  const close = t => {
    for (const e of world.live.values()) {
      e.gone = t
      world.done.push(e)
    }
    world.live.clear()
    world.end = t
  }
  const note = (e, t, s) => {
    if (!s) return
    const key = numbersAside(s)
    if (e.texts.some(x => x.key === key) || e.texts.length >= MAX_TEXTS) return
    e.texts.push({ t, text: s, key })
  }

  function handle (p) {
    const d = p.params
    lastT = p.t
    if (p.name === 'login' || p.name === 'respawn') return newWorld(p.t, p.name)
    if (p.name === 'player_info') {
      for (const e of d.data || []) {
        if (d.action.add_player && e.player) profiles[e.uuid] = { name: e.player.name, skin: skin(e.player.properties) }
      }
      return
    }
    if (!world) return
    switch (p.name) {
      case 'spawn_entity': {
        const profile = profiles[d.objectUUID]
        // A tab list entry makes it a player whatever the type tables say.
        const type = profile ? 'player' : (types[d.type] || '#' + d.type)
        const pos = [d.x, d.y, d.z].map(Number)
        world.live.set(d.entityId, {
          id: d.entityId,
          uuid: d.objectUUID,
          type,
          from: pos,
          pos: [...pos],
          yaw: angle(d.yaw),
          pitch: angle(d.pitch),
          moved: 0,
          hurt: false,
          t: p.t,
          gone: null,
          name: null,
          nameVisible: false,
          texts: [],
          vehicle: null,
          equipment: {},
          profileName: profile ? profile.name : null,
          skin: profile ? profile.skin : null
        })
        break
      }
      case 'entity_metadata': {
        const e = world.live.get(d.entityId)
        if (!e) break
        for (const m of d.metadata || []) {
          // Every entity's 2 is its custom name and 3 whether it shows; a text display has one component.
          if (m.key === 2 && m.type === 'optional_component') e.name = m.value == null ? null : text(m.value)
          else if (m.key === 3 && m.type === 'boolean') e.nameVisible = !!m.value
          else if (m.type === 'component' && e.type === 'text_display') note(e, p.t, text(m.value))
          else if (m.type === 'resolvable_profile' && m.value) e.skin = skin(m.value.properties) || e.skin
        }
        if (e.name && (e.nameVisible || e.type !== 'armor_stand')) note(e, p.t, e.name)
        break
      }
      case 'rel_entity_move':
      case 'entity_move_look': {
        const e = world.live.get(d.entityId)
        if (e) moveTo(e, [e.pos[0] + d.dX / 4096, e.pos[1] + d.dY / 4096, e.pos[2] + d.dZ / 4096])
        break
      }
      case 'sync_entity_position':
      case 'entity_teleport': {
        const e = world.live.get(d.entityId)
        if (e) moveTo(e, [d.x, d.y, d.z].map(Number))
        break
      }
      case 'set_passengers':
        for (const id of d.passengers || []) {
          const e = world.live.get(id)
          if (e) e.vehicle = d.entityId
        }
        break
      case 'entity_equipment': {
        const e = world.live.get(d.entityId)
        if (!e) break
        for (const { slot, item } of d.equipments || []) {
          const x = describe(item)
          const key = SLOTS[slot] || 'slot ' + slot
          if (x) e.equipment[key] = { id: x.id, name: x.name }
          else delete e.equipment[key]
        }
        break
      }
      // NPCs can't be hurt and don't die: what does is a mob, whatever it looks like.
      case 'hurt_animation':
      case 'damage_event':
      case 'entity_status': {
        const e = world.live.get(d.entityId)
        if (e && (p.name !== 'entity_status' || d.entityStatus === 2 || d.entityStatus === 3)) e.hurt = true
        break
      }
      case 'entity_destroy':
        for (const id of d.entityIds || []) {
          const e = world.live.get(id)
          if (!e) continue
          e.gone = p.t
          world.live.delete(id)
          world.done.push(e)
        }
        break
    }
  }

  const moveTo = (e, pos) => {
    e.pos = pos
    e.moved = Math.max(e.moved, Math.hypot(pos[0] - e.from[0], pos[1] - e.from[1], pos[2] - e.from[2]))
  }

  /**
   * The server and area SkyBlock's sidebar shows, for the world the player is in: the server id
   * after the date ("09/26/26 m12DQ"), and the area on the line after the time ("10:30am ☀",
   * then "Your Island"). Its icon is a resource pack glyph now (U+E067), "⏣" on older clients.
   */
  function area (sidebarText) {
    if (!world) return
    const lines = sidebarText.split('\n').slice(1).map(l => plain(l.replace(/^\s*\|/, '')).trim())
    const server = lines.map(l => l.match(/^\d\d\/\d\d\/\d\d\s+(\S+)/)).find(Boolean)
    if (server && !world.server) world.server = server[1]
    const time = lines.findIndex(l => /\b\d{1,2}:\d\d\s?(am|pm)\b/i.test(l))
    const here = time >= 0 && lines[time + 1] && lines[time + 1].replace(/[-⏣ф]/g, '').trim()
    if (here && !world.areas.includes(here)) world.areas.push(here)
  }

  function report () {
    if (world && world.end === null) close(lastT)
    const out = worlds.map(sortOut).filter(w => w.npcs.length || w.holograms.length || w.mobs.length || w.untagged.length)
    out.forEach((w, i) => { w.n = i + 1 })
    return { worlds: out, text: write(out), npcs: out.reduce((n, w) => n + w.npcs.length, 0), holograms: out.reduce((n, w) => n + w.holograms.length, 0) }
  }

  return { handle, area, report }
}

const overlaps = (a, b) => a.t <= b.gone && b.t <= a.gone
const inColumn = (a, b) => Math.abs(a.from[0] - b.from[0]) <= COLUMN && Math.abs(a.from[2] - b.from[2]) <= COLUMN

/** One world's entities sorted into NPCs, loose holograms and named mobs. */
function sortOut (w) {
  const all = w.done
  const tags = all.filter(e => e.texts.length && (e.type === 'armor_stand' || e.type === 'text_display'))
  const bodies = all.filter(e => !NOT_A_BODY.test(e.type))
  const byId = new Map(all.map(e => [e.id, e]))
  for (const b of bodies) b.tags = []
  const loose = []
  for (const tag of tags) {
    const vehicle = tag.vehicle != null && byId.get(tag.vehicle)
    let body = vehicle && vehicle.tags ? vehicle : null
    let best = Infinity
    for (const b of vehicle ? [] : bodies) {
      if (!overlaps(b, tag) || !inColumn(b, tag) || tag.from[1] < b.from[1] - 0.5 || tag.from[1] > b.from[1] + ABOVE) continue
      const off = Math.hypot(tag.from[0] - b.from[0], tag.from[2] - b.from[2])
      if (off < best) {
        best = off
        body = b
      }
    }
    if (body) body.tags.push(tag)
    else if (tag.moved <= STILL && !vehicle) loose.push(tag)
  }

  const npcs = []
  const untagged = []
  const mobs = new Map()
  for (const b of bodies) {
    if (b.type === 'player' && !isNpcUuid(b.uuid)) continue
    const lineEntities = [...(b.name && b.texts.length ? [b] : []), ...b.tags.sort((x, y) => y.from[1] - x.from[1])]
    const lines = lineEntities.map(e => e.texts[0].text)
    const mob = b.hurt || b.moved > STILL || lines.some(l => MOB_TAG.test(plain(l)))
    if (!mob && lines.length) npcs.push(entry(b, lineEntities))
    // A player with no name tag, standing still and never hurt: a mob nobody fought, or scenery.
    else if (!mob && (b.type === 'player' || b.type === 'mannequin')) untagged.push(entry(b, []))
    else if (lines.length) {
      const name = nameOf(lines.map(plain)) || plain(lines[0])
      const key = numbersAside(name) + '|' + b.type
      const m = mobs.get(key) || { name, type: b.type, count: 0 }
      m.count++
      mobs.set(key, m)
    }
  }

  // Tags standing on their own, stacked into holograms by column; numbers popping out of mobs aside.
  const holograms = []
  const shown = loose.filter(tag => tag.gone - tag.t >= BRIEF && !tag.texts.every(x => DAMAGE.test(plain(x.text)) || MOB_TAG.test(plain(x.text))))
  for (const tag of shown.sort((a, b) => b.from[1] - a.from[1])) {
    const stack = holograms.find(h => h.parts.some(x => inColumn(x, tag) && overlaps(x, tag)))
    if (stack) stack.parts.push(tag)
    else holograms.push({ parts: [tag] })
  }

  // Merged together: a body sent again under the same tags got them only the first time.
  const people = merge([...npcs, ...untagged])
  return {
    t: w.t,
    end: w.end,
    how: w.how,
    server: w.server,
    areas: w.areas,
    npcs: people.filter(n => n.lines.length),
    holograms: merge(holograms.map(h => entry(h.parts[h.parts.length - 1], h.parts, true))),
    untagged: people.filter(n => !n.lines.length),
    mobs: [...mobs.values()].sort((a, b) => b.count - a.count || a.name.localeCompare(b.name))
  }
}

/**
 * An NPC's name among its tag lines: the one just above "CLICK" (under it a header like
 * "⚑ GUIDES" may stand), else the first that isn't "CLICK".
 */
function nameOf (lines) {
  const click = lines.findLastIndex(l => CLICK.test(l))
  if (click > 0 && lines[click - 1]) return lines[click - 1]
  return lines.find(l => l && !CLICK.test(l)) || null
}

/** What's reported of an NPC (or, with `hologram`, of a stack of tags standing on their own). */
function entry (e, lineEntities, hologram) {
  const lines = lineEntities.map(x => x.texts[0].text)
  const changes = []
  lineEntities.forEach((x, line) => x.texts.slice(1).forEach(c => changes.push({ t: c.t, line, text: c.text })))
  const out = {
    x: round(e.from[0]),
    y: round(e.from[1]),
    z: round(e.from[2]),
    lines,
    changes: changes.sort((a, b) => a.t - b.t),
    seen: [Math.min(...lineEntities.map(x => x.t), e.t), Math.max(...lineEntities.map(x => x.gone), e.gone)]
  }
  if (hologram) return out
  return {
    type: e.type,
    uuid: e.uuid,
    name: nameOf(lines.map(plain)),
    ...out,
    yaw: e.yaw,
    pitch: e.pitch,
    profileName: e.profileName || undefined,
    skin: e.skin || undefined,
    equipment: Object.keys(e.equipment).length ? e.equipment : undefined
  }
}

/**
 * The same NPC or hologram seen more than once (it went out of range and came back, or its body
 * was sent again under the same name tags) as one: NPCs by type, spot and skin; holograms by spot
 * and first line.
 */
function merge (entries) {
  const out = new Map()
  for (const e of entries) {
    const spot = [Math.round(e.x * 2), Math.round(e.y * 2), Math.round(e.z * 2)]
    const key = (e.type ? [e.type, ...spot, e.skin ? e.skin.url : ''] : [...spot, numbersAside(e.lines[0] || '')]).join('|')
    const had = out.get(key)
    if (!had) {
      out.set(key, e)
      continue
    }
    if (!had.lines.length && e.lines.length) {
      had.lines = e.lines
      had.name = e.name
    }
    had.seen = [Math.min(had.seen[0], e.seen[0]), Math.max(had.seen[1], e.seen[1])]
    for (const c of e.changes) {
      if (!had.changes.some(x => x.line === c.line && numbersAside(x.text) === numbersAside(c.text))) had.changes.push(c)
    }
  }
  return [...out.values()].sort((a, b) => a.seen[0] - b.seen[0] || a.x - b.x || a.z - b.z)
}

const where = e => `${e.x} ${e.y} ${e.z}`
const facing = yaw => COMPASS[Math.round((((yaw % 360) + 360) % 360) / 45) % 8]
const skinLine = s => `skin ${s.url || '(no URL)'}${s.slim ? ' (slim)' : ''}${s.signature ? '' : ', unsigned'}`
const wears = equipment => 'wears ' + Object.entries(equipment).map(([slot, x]) => `${slot}: ${x.id}${x.name ? ' ' + x.name : ''}`).join('; ')

function write (worlds) {
  const out = []
  for (const w of worlds) {
    const place = [w.server && 'server ' + w.server, w.areas.length && w.areas.join(', ')].filter(Boolean).join(': ')
    out.push(`===== world ${w.n} (${w.how} at ${clock(w.t)}, until ${clock(w.end)})${place ? ' ' + place : ''}`)
    out.push(`NPCs (${w.npcs.length}):`)
    for (const n of w.npcs) {
      out.push(`  ${n.name || '?'} [${n.type}] at ${where(n)}, facing ${facing(n.yaw)} (${n.yaw}°)   ${clock(n.seen[0])} - ${clock(n.seen[1])}`)
      out.push('      ' + n.lines.join(' / '))
      for (const c of n.changes) out.push(`      ${clock(c.t)} line ${c.line + 1} became ${c.text}`)
      if (n.skin) out.push('      ' + skinLine(n.skin))
      if (n.equipment) out.push('      ' + wears(n.equipment))
    }
    out.push(`Holograms (${w.holograms.length}):`)
    for (const h of w.holograms) {
      out.push(`  at ${where(h)}: ${h.lines.join(' / ')}   ${clock(h.seen[0])} - ${clock(h.seen[1])}`)
      for (const c of h.changes) out.push(`      ${clock(c.t)} line ${c.line + 1} became ${c.text}`)
    }
    if (w.untagged.length) {
      out.push(`Players with no name tag, still and never hurt: mobs nobody fought, or scenery (${w.untagged.length}):`)
      for (const n of w.untagged) {
        out.push(`  at ${where(n)}, facing ${facing(n.yaw)}   ${clock(n.seen[0])} - ${clock(n.seen[1])}` +
          (n.skin ? '   ' + skinLine(n.skin) : '') + (n.equipment ? '   ' + wears(n.equipment) : ''))
      }
    }
    if (w.mobs.length) {
      out.push(`Named mobs (${w.mobs.reduce((n, m) => n + m.count, 0)}):`)
      for (const m of w.mobs) out.push(`  ${String(m.count).padStart(4)}x ${m.name} [${m.type}]`)
    }
    out.push('')
  }
  return out.join('\n') + '\n'
}

module.exports = { track, skin }
