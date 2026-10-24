'use strict'
const test = require('node:test')
const assert = require('node:assert')
const { track, skin } = require('../lib/entities')

const TYPES = { 5: 'armor_stand', 132: 'text_display', 140: 'villager', 150: 'zombie' }
const NPC = '9cd147da-122d-2a39-8f18-649be597111b'
const NPC2 = '62d4b293-35b7-2de6-9a27-76612932ba44'
const PLAYER = '1d0e1f81-9783-4882-8085-4cd8e7701a07'
const texture = (url, slim) => Buffer.from(JSON.stringify({ textures: { SKIN: { url, ...(slim ? { metadata: { model: 'slim' } } : {}) } } })).toString('base64')

/** Feeds packets made by `script` to a tracker and returns its report. */
function run (script) {
  const npcs = track(TYPES, item => item && item.itemCount ? { id: 'item#' + item.itemId, count: item.itemCount, name: null, lore: [] } : null)
  let t = 0
  const at = (name, params, ms = 100) => {
    t += ms
    npcs.handle({ t, state: 'play', name, params })
  }
  const api = {
    login: () => at('login', {}),
    profile: (uuid, name, url, slim) => at('player_info', {
      action: { add_player: true },
      data: [{ uuid, player: { name, properties: [{ name: 'textures', value: texture(url, slim), signature: 'sig' }] } }]
    }),
    spawn: (id, type, x, y, z, uuid = '00000000-0000-4000-8000-' + String(id).padStart(12, '0'), yaw = 64) =>
      at('spawn_entity', { entityId: id, objectUUID: uuid, type, x, y, z, yaw, pitch: 0 }),
    tag: (id, x, y, z, name) => {
      api.spawn(id, 5, x, y, z)
      at('entity_metadata', { entityId: id, metadata: [{ key: 2, type: 'optional_component', value: name }, { key: 3, type: 'boolean', value: true }] }, 0)
    },
    rename: (id, name, ms) => at('entity_metadata', { entityId: id, metadata: [{ key: 2, type: 'optional_component', value: name }] }, ms),
    move: (id, dX, dY, dZ) => at('rel_entity_move', { entityId: id, dX, dY, dZ }),
    hurt: id => at('hurt_animation', { entityId: id, yaw: 0 }),
    destroy: ids => at('entity_destroy', { entityIds: ids }),
    wait: ms => at('keep_alive', {}, ms),
    sidebar: text => npcs.area(text)
  }
  script(api)
  return npcs.report()
}

test('an NPC is a v2 player standing still under its name tags, named by the line above CLICK', () => {
  const r = run(s => {
    s.login()
    s.profile(NPC, '0292r41p7b', 'http://textures.minecraft.net/texture/abc', true)
    s.spawn(1, 999, 8.5, 75, 10.5, NPC, -64)
    s.tag(2, 8.5, 75.66, 10.5, '§a§l⚑ GUIDES')
    s.tag(3, 8.5, 75.28, 10.5, 'Biblio')
    s.tag(4, 8.5, 74.94, 10.5, '§e§lCLICK')
    s.wait(5000)
  })
  assert.strictEqual(r.npcs, 1)
  const npc = r.worlds[0].npcs[0]
  assert.strictEqual(npc.name, 'Biblio')
  assert.strictEqual(npc.type, 'player')
  assert.deepStrictEqual(npc.lines, ['&a&l⚑ GUIDES', 'Biblio', '&e&lCLICK'])
  assert.deepStrictEqual([npc.x, npc.y, npc.z, npc.yaw], [8.5, 75, 10.5, -90])
  assert.deepStrictEqual(npc.skin, { value: npc.skin.value, signature: 'sig', url: 'http://textures.minecraft.net/texture/abc', slim: true })
  assert.match(r.text, /Biblio \[player\] at 8\.5 75 10\.5, facing east/)
})

test('real players, and mobs that are hurt or walk off, are not NPCs', () => {
  const r = run(s => {
    s.login()
    s.profile(PLAYER, 'ICoding', 'http://textures.minecraft.net/texture/me')
    s.spawn(1, 999, 0, 70, 0, PLAYER)
    s.tag(2, 0, 72, 0, '§6✦ 2-Year Hypixel Veteran')
    s.spawn(3, 150, 20, 70, 20)
    s.tag(4, 20, 71, 20, 'Zombie Grunt')
    s.hurt(3)
    s.spawn(5, 150, 40, 70, 40)
    s.tag(6, 40, 71, 40, 'Zombie Grunt')
    s.move(5, 4096 * 3, 0, 0)
    s.wait(5000)
  })
  assert.strictEqual(r.npcs, 0)
  assert.deepStrictEqual(r.worlds[0].mobs, [{ name: 'Zombie Grunt', type: 'zombie', count: 2 }])
  assert.strictEqual(r.holograms, 0)
})

test('tags on their own stack into holograms; damage numbers and quick ones are left out', () => {
  const r = run(s => {
    s.login()
    s.tag(1, 3.5, 76.5, 41.5, '§e§lParkour Challenge')
    s.tag(2, 3.5, 76.13, 41.5, '§a§lStart')
    s.tag(3, 10, 70, 10, '§f✧9§e,§60§c39✧')
    s.wait(2000)
    s.tag(4, 20, 70, 20, '§6What?')
    s.destroy([4])
    s.wait(5000)
  })
  assert.strictEqual(r.holograms, 1)
  const h = r.worlds[0].holograms[0]
  assert.deepStrictEqual(h.lines, ['&e&lParkour Challenge', '&a&lStart'])
})

test('a tag that changes in more than numbers lists its new texts', () => {
  const r = run(s => {
    s.login()
    s.profile(NPC, 'evyg610806', 'http://textures.minecraft.net/texture/d')
    s.spawn(1, 999, 8.5, 75, -3.5, NPC)
    s.tag(2, 8.5, 75.66, -3.5, '§bThe Delivery Man')
    s.tag(3, 8.5, 75.28, -3.5, '§e§lRIGHT CLICK')
    s.tag(4, 8.5, 74.94, -3.5, '§f6 Deliveries!')
    s.rename(4, '§c7 Deliveries!', 1000)
    s.rename(4, '§bFree Daily Reward!', 1000)
    s.wait(5000)
  })
  const npc = r.worlds[0].npcs[0]
  assert.strictEqual(npc.name, 'The Delivery Man')
  assert.deepStrictEqual(npc.changes.map(c => [c.line, c.text]), [[2, '&bFree Daily Reward!']])
})

test('an NPC sent again at the same spot is one NPC, and a player with no tags is listed apart', () => {
  const r = run(s => {
    s.login()
    s.profile(NPC, 'a', 'http://textures.minecraft.net/texture/mort')
    s.profile(NPC2, 'b', 'http://textures.minecraft.net/texture/mort')
    s.spawn(1, 999, -141.5, 69, -88.5, NPC)
    s.tag(2, -141.5, 71, -88.5, '§bMort')
    s.tag(3, -141.5, 70.7, -88.5, '§e§lCLICK')
    s.destroy([1])
    s.spawn(4, 999, -141.5, 69, -88.5, NPC2)
    s.wait(5000)
    s.destroy([2, 3])
    s.profile('aaaaaaaa-bbbb-2ccc-8ddd-eeeeeeeeeeee', 'c', 'http://textures.minecraft.net/texture/far')
    s.spawn(5, 999, 30, 70, 30, 'aaaaaaaa-bbbb-2ccc-8ddd-eeeeeeeeeeee')
    s.wait(1000)
  })
  const w = r.worlds[0]
  assert.deepStrictEqual(w.npcs.map(n => n.name), ['Mort'])
  assert.strictEqual(w.untagged.length, 1)
  assert.strictEqual(w.untagged[0].x, 30)
})

test('a mob standing still under its tags, like Jerry, is an NPC', () => {
  const r = run(s => {
    s.login()
    s.spawn(1, 140, 12.5, 99, 37.5)
    s.tag(2, 12.5, 100.6, 37.5, 'Jerry')
    s.tag(3, 12.5, 100.3, 37.5, '§e§lCLICK')
    s.wait(5000)
  })
  assert.deepStrictEqual(r.worlds[0].npcs.map(n => [n.name, n.type]), [['Jerry', 'villager']])
})

test('text displays are holograms', () => {
  const npcs = track(TYPES, () => null)
  npcs.handle({ t: 0, state: 'play', name: 'login', params: {} })
  npcs.handle({ t: 10, state: 'play', name: 'spawn_entity', params: { entityId: 7, objectUUID: 'x', type: 132, x: 1, y: 2, z: 3, yaw: 0, pitch: 0 } })
  npcs.handle({ t: 10, state: 'play', name: 'entity_metadata', params: { entityId: 7, metadata: [{ key: 23, type: 'component', value: { text: 'Welcome', color: 'gold' } }] } })
  npcs.handle({ t: 5000, state: 'play', name: 'keep_alive', params: {} })
  const r = npcs.report()
  assert.deepStrictEqual(r.worlds[0].holograms[0].lines, ['&6Welcome'])
})

test('worlds are named by the sidebar: server after the date, area after the time', () => {
  const r = run(s => {
    s.login()
    s.tag(1, 0, 70, 0, 'Sign')
    s.sidebar('[&e&lSKYBLOCK]\n  | &709/26/26 &8m12&v&8DQ\n  | &f Early Autumn 8th\n  |  &710:30am &e☀\n  |  &7\uE067 &aYour Isla&q&and\n  | &fPurse: &657,690')
    s.wait(5000)
  })
  assert.strictEqual(r.worlds[0].server, 'm12DQ')
  assert.deepStrictEqual(r.worlds[0].areas, ['Your Island'])
  assert.match(r.text, /server m12DQ: Your Island/)
})

test('another line that reads like a time doesn\'t name the area: the line with the icon does', () => {
  const r = run(s => {
    s.login()
    s.tag(1, 0, 70, 0, 'Sign')
    s.sidebar('[SKYBLOCK]\n  | 8:00am\n  |  8:00am ☀\n  |  \uE067 Test Island\n  | Purse: 0')
    s.wait(5000)
  })
  assert.deepStrictEqual(r.worlds[0].areas, ['Test Island'])
})

test('a skin without the usual JSON still has its property', () => {
  assert.deepStrictEqual(skin([{ name: 'textures', value: 'not base64 json' }]), { value: 'not base64 json', signature: null })
  assert.strictEqual(skin([{ name: 'other', value: 'x' }]), null)
})
