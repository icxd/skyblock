'use strict'
const test = require('node:test')
const assert = require('node:assert')
const { soundName, describe, collect } = require('../lib/sounds')

// 26.2's ids for these (the registry report's protocol ids).
const TABLE = { 1169: 'block.note_block.pling', 1701: 'entity.villager.yes' }
const PLAYER = [7.5, 100, 7.5]

test('a sound is named by its registry id, or by the name sent with it', () => {
  assert.strictEqual(soundName({ soundId: 1169 }, TABLE), 'block.note_block.pling')
  assert.strictEqual(soundName({ soundId: 5 }, TABLE), '#5')
  assert.strictEqual(soundName({ data: { soundName: 'ui.toast.challenge_complete' } }, TABLE), 'ui.toast.challenge_complete')
  assert.strictEqual(soundName({ data: { soundName: 'minecraft:ui.toast.challenge_complete' } }, TABLE), 'ui.toast.challenge_complete')
})

test("Hypixel's second copy of a sound is the same sound", () => {
  const sounds = collect(TABLE)
  const pling = t => sounds.played(t, { soundId: 1169 }, 'block', [7.5, 100, 7.5], 1, 0.492, PLAYER)
  assert.ok(pling(1000))
  assert.strictEqual(pling(1000), null, 'the copy')
  // Spelled the other way, as Hypixel sends a sound that comes by name.
  assert.ok(sounds.played(1000, { data: { soundName: 'ui.toast.challenge_complete' } }, 'master', [7.5, 100, 7.5], 0.3, 1.492, PLAYER))
  assert.strictEqual(sounds.played(1000, { data: { soundName: 'minecraft:ui.toast.challenge_complete' } }, 'master', [7.5, 100, 7.5], 0.3, 1.492, PLAYER), null)
  assert.ok(pling(1250), 'played again later')
  assert.ok(sounds.played(1250, { soundId: 1169 }, 'block', [20, 100, 7.5], 1, 0.492, PLAYER), 'somewhere else')
  assert.strictEqual(sounds.heard.length, 4)
})

test('a sound played where the player stands is one for them; one from elsewhere is the world', () => {
  const sounds = collect(TABLE)
  const voice = sounds.played(0, { soundId: 1701 }, 'neutral', [7.5, 100, 32.5], 1, 1, [7.6, 100, 31.4])
  assert.ok(voice.atPlayer)
  assert.strictEqual(describe(voice), 'neutral, volume 1, pitch 1')
  const far = sounds.played(0, { soundId: 1169 }, 'block', [40, 70, 7.5], 1, 0.492, PLAYER)
  assert.strictEqual(far.atPlayer, false)
  assert.strictEqual(Math.round(far.distance), 44)
  const unknown = sounds.played(0, { soundId: 1169 }, 'block', [7.5, 100, 7.5], 1, 0.492, null)
  assert.strictEqual(unknown.distance, null)
  assert.strictEqual(unknown.atPlayer, false)
})
