'use strict'
// The sounds a recording's server played. Hypixel sends each one twice (the second the same, or
// with its name spelled "minecraft:..."), so a repeat at the same moment and place is one sound.
// Its own sounds (an NPC's voice, NEW OBJECTIVE's note, a menu's click) it plays where the player
// stands; the rest come from wherever they happened (other players, the world).

// The same sound this soon after, in the same place, is its repeat (ms).
const REPEAT = 100
// Played this close to the player: one the server played for them (blocks).
const AT_PLAYER = 2

const round = v => Math.round(v * 100) / 100

/** A sound's name: its registry id's (0-based, as the decoder gives it), or the one sent with it. */
function soundName (holder, table) {
  if (holder && holder.soundId != null) return table[holder.soundId] || '#' + holder.soundId
  const sent = holder && holder.data && (holder.data.soundName || holder.data.resource)
  return sent ? String(sent).replace(/^minecraft:/, '') : '?'
}

/** "block, volume 1, pitch 0.49". */
function describe (s) {
  return `${s.category}, volume ${round(s.volume)}, pitch ${round(s.pitch)}`
}

/** Collects sounds as they're played; `played` says whether one is new (not a repeat) and near the player. */
function collect (table) {
  const heard = []
  let recent = []
  /**
   * A sound played at `pos` (blocks; null if unknown), the player then at `player` (null if unknown). Null for a
   * repeat; else the sound, with its distance from the player (null if unknown).
   */
  function played (t, holder, category, pos, volume, pitch, player) {
    const name = soundName(holder, table)
    recent = recent.filter(s => t - s.t <= REPEAT)
    const samePlace = s => s.pos === pos || (s.pos && pos && s.pos.every((v, i) => Math.abs(v - pos[i]) < 0.01))
    const same = s => s.name === name && s.category === category && s.volume === volume && s.pitch === pitch && samePlace(s)
    if (recent.some(same)) return null
    const distance = player && pos ? Math.hypot(pos[0] - player[0], pos[1] - player[1], pos[2] - player[2]) : null
    const s = { t, name, category, volume, pitch, pos, distance, atPlayer: distance != null && distance <= AT_PLAYER }
    recent.push(s)
    heard.push(s)
    return s
  }
  return { played, heard }
}

module.exports = { soundName, describe, collect, AT_PLAYER }
