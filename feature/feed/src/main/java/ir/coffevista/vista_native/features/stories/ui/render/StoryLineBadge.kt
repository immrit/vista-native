package ir.coffevista.vista_native.features.stories.ui.render

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal data class LineRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

internal sealed interface BadgeCommand {
    data class MoveTo(val x: Float, val y: Float) : BadgeCommand
    data class LineTo(val x: Float, val y: Float) : BadgeCommand
    data class CubicTo(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val x3: Float, val y3: Float,
    ) : BadgeCommand

    data object Close : BadgeCommand
}

internal object StoryLineBadge {
    private const val KAPPA = 0.5522847f
    private const val EPS = 0.01f

    fun build(lines: List<LineRect>, radius: Float): List<BadgeCommand> {
        val usable = normalise(lines, radius)
        if (usable.isEmpty()) return emptyList()
        val n = usable.size

        val y = FloatArray(n + 1)
        y[0] = usable.first().top
        for (k in 1 until n) y[k] = (usable[k - 1].bottom + usable[k].top) / 2f
        y[n] = usable.last().bottom

        val right = FloatArray(n) { usable[it].right }
        val left = FloatArray(n) { usable[it].left }
        smooth(right, radius, takeMax = true)
        smooth(left, radius, takeMax = false)

        val points = ArrayList<Pair<Float, Float>>()
        points += right[0] to y[0]
        for (i in 0 until n) {
            points += right[i] to y[i + 1]
            if (i < n - 1 && abs(right[i + 1] - right[i]) > EPS) points += right[i + 1] to y[i + 1]
        }
        points += left[n - 1] to y[n]
        for (i in n - 1 downTo 0) {
            points += left[i] to y[i]
            if (i > 0 && abs(left[i - 1] - left[i]) > EPS) points += left[i - 1] to y[i]
        }

        val polygon = simplify(points)
        if (polygon.size < 3) return emptyList()
        return round(polygon, radius)
    }

    private fun normalise(lines: List<LineRect>, radius: Float): List<LineRect> {
        if (lines.isEmpty()) return emptyList()
        val minWidth = radius * 0.5f
        val firstValid = lines.firstOrNull { it.right - it.left > minWidth } ?: return emptyList()
        var previous = firstValid
        return lines.map { line ->
            if (line.right - line.left > minWidth) {
                previous = line
                line
            } else {
                LineRect(previous.left, line.top, previous.right, line.bottom)
            }
        }
    }

    private fun smooth(edges: FloatArray, radius: Float, takeMax: Boolean) {
        val tolerance = radius * 1.2f
        repeat(2) {
            for (i in 0 until edges.size - 1) merge(edges, i, i + 1, tolerance, takeMax)
            for (i in edges.size - 1 downTo 1) merge(edges, i, i - 1, tolerance, takeMax)
        }
    }

    private fun merge(edges: FloatArray, a: Int, b: Int, tolerance: Float, takeMax: Boolean) {
        if (abs(edges[a] - edges[b]) < tolerance) {
            val value = if (takeMax) max(edges[a], edges[b]) else min(edges[a], edges[b])
            edges[a] = value
            edges[b] = value
        }
    }

    private fun simplify(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val deduped = ArrayList<Pair<Float, Float>>()
        for (p in points) {
            val last = deduped.lastOrNull()
            if (last == null || abs(last.first - p.first) > EPS || abs(last.second - p.second) > EPS) deduped += p
        }
        if (deduped.size > 1) {
            val f = deduped.first()
            val l = deduped.last()
            if (abs(f.first - l.first) <= EPS && abs(f.second - l.second) <= EPS) deduped.removeAt(deduped.lastIndex)
        }
        var current: List<Pair<Float, Float>> = deduped
        var changed = true
        while (changed && current.size >= 3) {
            changed = false
            val result = ArrayList<Pair<Float, Float>>()
            for (i in current.indices) {
                val prev = current[(i - 1 + current.size) % current.size]
                val cur = current[i]
                val next = current[(i + 1) % current.size]
                val cross = (cur.first - prev.first) * (next.second - cur.second) -
                    (cur.second - prev.second) * (next.first - cur.first)
                if (abs(cross) <= EPS) changed = true else result += cur
            }
            current = result
        }
        return current
    }

    private fun round(polygon: List<Pair<Float, Float>>, radius: Float): List<BadgeCommand> {
        val n = polygon.size
        class Corner(val ax: Float, val ay: Float, val vx: Float, val vy: Float, val bx: Float, val by: Float)

        val corners = (0 until n).map { i ->
            val prev = polygon[(i - 1 + n) % n]
            val cur = polygon[i]
            val next = polygon[(i + 1) % n]
            val d1 = hypot(prev.first - cur.first, prev.second - cur.second)
            val d2 = hypot(next.first - cur.first, next.second - cur.second)
            val r = min(radius, min(d1, d2) / 2f)
            Corner(
                ax = cur.first + (prev.first - cur.first) / d1 * r,
                ay = cur.second + (prev.second - cur.second) / d1 * r,
                vx = cur.first,
                vy = cur.second,
                bx = cur.first + (next.first - cur.first) / d2 * r,
                by = cur.second + (next.second - cur.second) / d2 * r,
            )
        }

        val commands = ArrayList<BadgeCommand>(n * 2 + 2)
        commands += BadgeCommand.MoveTo(corners[0].ax, corners[0].ay)
        corners.forEachIndexed { index, c ->
            if (index > 0) commands += BadgeCommand.LineTo(c.ax, c.ay)
            commands += BadgeCommand.CubicTo(
                x1 = c.ax + (c.vx - c.ax) * KAPPA,
                y1 = c.ay + (c.vy - c.ay) * KAPPA,
                x2 = c.bx + (c.vx - c.bx) * KAPPA,
                y2 = c.by + (c.vy - c.by) * KAPPA,
                x3 = c.bx,
                y3 = c.by,
            )
        }
        commands += BadgeCommand.Close
        return commands
    }
}
