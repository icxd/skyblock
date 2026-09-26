'use strict'
// A readable account of a recording: what the player saw, in order.
//   timeline.txt  chat, action bar (when it changes in more than numbers), titles, boss bar,
//                 server and world changes, teleports, menus opened, items put in the inventory
//   sidebar.txt   the sidebar each time it settles into something new (numbers aside)
//   tab.txt       the tab list the same way, in slot order
//   gui.txt       every menu's contents: slots, item names and lore
const fs = require('fs')
const path = require('path')
const { text, plain } = require('./text')
const { packets } = require('./recording')

/** Time since the recording started, as mm:ss.s. */
function clock (ms) {
  const s = ms / 1000
  return String(Math.floor(s / 60)).padStart(2, '0') + ':' + (s % 60).toFixed(1).padStart(4, '0')
}

function decode (recording, dec, items, outDir) {
  const out = { main: [], sidebar: [], tab: [], gui: [] }
  const numbersAside = s => plain(s).replace(/[\d,.]+/g, '#')

  const itemInfo = item => {
    if (!item || !item.itemCount) return null
    let name = null
    let lore = []
    for (const c of item.components || []) {
      if (c.type === 'custom_name' || c.type === 'item_name') name = name || text(c.data)
      if (c.type === 'lore') lore = (c.data || []).map(l => text(l))
    }
    return { id: items[item.itemId] || '#' + item.itemId, count: item.itemCount, name, lore }
  }
  const describe = x => x.id + ' x' + x.count + ' ' + (x.name || '') + (x.lore.length ? '\n      ' + x.lore.join('\n      ') : '')

  // Scoreboard: objectives, scores, teams (sidebar lines are team prefix + holder + suffix, or the
  // score's own display name).
  const objectives = {}
  const scores = {}
  const teams = {}
  const holderTeam = {}
  let sidebarObjective = null
  const sidebar = () => {
    if (!sidebarObjective) return null
    const lines = Object.entries(scores[sidebarObjective] || {}).sort((a, b) => b[1].value - a[1].value).map(([holder, s]) => {
      if (s.display) return text(s.display)
      const team = teams[holderTeam[holder]]
      // Holders are often invisible codes (§a, §v, ...) that split a line between prefix and suffix.
      return team ? text(team.prefix) + holder.replace(/§/g, '&') + text(team.suffix) : holder.replace(/§/g, '&')
    })
    return '[' + text(objectives[sidebarObjective]) + ']\n' + lines.map(l => '  | ' + l).join('\n')
  }

  // Tab list: servers fill it with fake players whose names (like !A-a) set the order.
  const tab = {}
  let header = ''
  let footer = ''
  const tabList = () => {
    const rows = Object.values(tab).filter(e => e.listed !== false)
      .sort((a, b) => (a.name || '').localeCompare(b.name || ''))
      .map(e => '  ' + (e.name || '?').padEnd(6) + ' ' + (e.display != null ? text(e.display) : ''))
    return 'header: ' + header.replace(/\n/g, ' / ') + '\nfooter: ' + footer.replace(/\n/g, ' / ') + '\n' + rows.join('\n')
  }

  // Snapshots are written once they've settled, so a sidebar being built line by line shows once;
  // and when they change in more than numbers, or anything changed and half a minute has passed.
  const settled = {
    sidebar: { pending: null, last: null, lastText: null, lastT: -Infinity, wait: 500 },
    tab: { pending: null, last: null, lastText: null, lastT: -Infinity, wait: 1000 }
  }
  const flush = (kind, now) => {
    const s = settled[kind]
    if (!s.pending || (now !== undefined && now - s.pending.t <= s.wait)) return
    const norm = numbersAside(s.pending.text)
    if (norm !== s.last || (s.pending.text !== s.lastText && s.pending.t - s.lastT >= 30_000)) {
      s.last = norm
      s.lastText = s.pending.text
      s.lastT = s.pending.t
      if (kind === 'sidebar') {
        out.sidebar.push(clock(s.pending.t) + '\n' + s.pending.text)
        out.main.push({ t: s.pending.t, line: '[sidebar] changed (see sidebar.txt)' })
      } else {
        out.tab.push('\n===== ' + clock(s.pending.t) + '\n' + s.pending.text)
      }
    }
    s.pending = null
  }

  let openWindow = null
  let lastBar = null
  let lastBoss = null
  const inventory = {}
  const stats = { packets: 0, errors: 0 }
  // What each inventory slot holds, logged when it changes.
  const noteInventory = (t, slot, x) => {
    const key = x ? (x.name || x.id) : null
    if (!key) {
      delete inventory[slot]
      return
    }
    if (inventory[slot] === key) return
    inventory[slot] = key
    out.main.push({ t, line: `[inv] slot ${slot} = ${plain(key)}` })
  }

  for (const p of packets(recording, dec)) {
    stats.packets++
    flush('sidebar', p.t)
    flush('tab', p.t)
    if (p.error) {
      stats.errors++
      continue
    }
    const { t, name, params: d } = p
    const log = line => out.main.push({ t, line })
    if (p.state === 'configuration' && name === 'finish_configuration') continue
    if (p.state === 'play' && name === 'start_configuration') log('[server] switching servers')
    if (p.state !== 'play') continue

    switch (name) {
      case 'system_chat': {
        const s = text(d.content)
        if (!d.isActionBar) log('[chat] ' + s)
        else if (numbersAside(s) !== lastBar) {
          lastBar = numbersAside(s)
          log('[actionbar] ' + s)
        }
        break
      }
      case 'player_chat': log('[chat] ' + text(d.networkName) + ': ' + (d.unsignedChatContent ? text(d.unsignedChatContent) : d.plainMessage)); break
      case 'profileless_chat': log('[chat] ' + text(d.name) + ': ' + text(d.message)); break
      case 'action_bar': log('[actionbar] ' + text(d.text)); break
      case 'set_title_text': log('[title] ' + text(d.text)); break
      case 'set_title_subtitle': log('[subtitle] ' + text(d.text)); break
      case 'set_title_time': log(`[title timing] in ${d.fadeIn} stay ${d.stay} out ${d.fadeOut} ticks`); break
      case 'clear_titles': log('[title] cleared'); break
      case 'boss_bar':
        if (d.title) {
          const s = text(d.title)
          if (numbersAside(s) !== lastBoss) {
            lastBoss = numbersAside(s)
            log('[bossbar] ' + s)
          }
        }
        break
      case 'login':
      case 'respawn': {
        const w = d.worldState || {}
        log(`[world] ${name}: ${w.name || d.worldName} (${w.gamemode ?? ''})`)
        break
      }
      case 'position': log('[teleport] ' + [d.x, d.y, d.z].map(v => (+v).toFixed(1)).join(', ')); break
      case 'kick_disconnect': log('[disconnect] ' + text(d.reason)); break
      case 'transfer': log('[transfer] ' + JSON.stringify(d)); break

      case 'playerlist_header':
        header = text(d.header)
        footer = text(d.footer)
        settled.tab.pending = { t, text: tabList() }
        break
      case 'player_info': {
        const a = d.action || {}
        for (const e of d.data || []) {
          const entry = tab[e.uuid] || (tab[e.uuid] = {})
          if (a.add_player && e.player) entry.name = e.player.name
          if (a.update_listed) entry.listed = e.listed
          if (a.update_display_name) entry.display = e.displayName
        }
        settled.tab.pending = { t, text: tabList() }
        break
      }
      case 'player_remove':
        for (const u of d.players || []) delete tab[u]
        settled.tab.pending = { t, text: tabList() }
        break

      case 'scoreboard_objective':
        if (d.action === 1) delete objectives[d.name]
        else objectives[d.name] = d.displayText
        break
      case 'scoreboard_display_objective':
        if (d.position === 1 || d.position === 'sidebar') sidebarObjective = d.name
        break
      case 'scoreboard_score':
        (scores[d.scoreName] ||= {})[d.itemName] = { value: d.value, display: d.display_name ?? d.displayName }
        break
      case 'reset_score': {
        const holder = d.entity_name || d.entityName
        const objective = d.objective_name || d.objectiveName
        if (objective) delete (scores[objective] || {})[holder]
        else for (const o in scores) delete scores[o][holder]
        break
      }
      case 'teams':
        if (d.mode === 'remove') {
          delete teams[d.team]
          break
        }
        if (d.mode === 'add' || d.mode === 'change') teams[d.team] = { prefix: d.prefix, suffix: d.suffix }
        for (const holder of d.players || []) {
          if (d.mode === 'leave') delete holderTeam[holder]
          else holderTeam[holder] = d.team
        }
        break

      case 'open_window':
        // Generic chest menus are types 0-5 (9x1 to 9x6); their last 36 slots are the player's inventory.
        openWindow = { id: d.windowId, title: text(d.windowTitle), size: d.inventoryType <= 5 ? 9 * (d.inventoryType + 1) : 99 }
        log('[gui] opened "' + plain(openWindow.title) + '"')
        out.gui.push('\n=== ' + clock(t) + ' opened: ' + openWindow.title)
        break
      case 'close_window':
        log('[gui] closed')
        openWindow = null
        break
      case 'window_items':
        if (d.windowId === 0) {
          d.items.forEach((item, slot) => noteInventory(t, slot, itemInfo(item)))
        } else if (openWindow && d.windowId === openWindow.id) {
          out.gui.push('--- ' + clock(t) + ' contents of "' + openWindow.title + '"')
          d.items.forEach((item, slot) => {
            const x = itemInfo(item)
            if (x && slot < d.items.length - 36) out.gui.push('  [' + slot + '] ' + describe(x))
          })
        }
        break
      case 'set_slot': {
        const x = itemInfo(d.item)
        if (openWindow && d.windowId === openWindow.id) {
          if (d.slot < openWindow.size && !(x && /stained_glass_pane$/.test(x.id) && !x.lore.length)) {
            out.gui.push('  ' + clock(t) + ' slot ' + d.slot + ' := ' + (x ? describe(x) : 'empty'))
          }
        } else if (d.windowId === 0 || d.windowId === -2) {
          noteInventory(t, d.slot, x)
        }
        break
      }
    }
    if (['teams', 'scoreboard_score', 'reset_score', 'scoreboard_objective', 'scoreboard_display_objective'].includes(name)) {
      const s = sidebar()
      if (s) settled.sidebar.pending = { t, text: s }
    }

  }
  flush('sidebar')
  flush('tab')

  fs.mkdirSync(outDir, { recursive: true })
  const main = out.main.map((e, i) => ({ ...e, i })).sort((a, b) => a.t - b.t || a.i - b.i).map(e => clock(e.t) + '  ' + e.line)
  fs.writeFileSync(path.join(outDir, 'timeline.txt'), main.join('\n') + '\n')
  fs.writeFileSync(path.join(outDir, 'sidebar.txt'), out.sidebar.join('\n\n') + '\n')
  fs.writeFileSync(path.join(outDir, 'tab.txt'), out.tab.join('\n') + '\n')
  fs.writeFileSync(path.join(outDir, 'gui.txt'), out.gui.join('\n') + '\n')
  return { ...stats, timeline: main.length, sidebars: out.sidebar.length, tabs: out.tab.length }
}

module.exports = { decode, clock }
