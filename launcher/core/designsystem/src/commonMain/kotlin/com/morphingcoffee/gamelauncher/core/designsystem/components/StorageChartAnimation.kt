package com.morphingcoffee.gamelauncher.core.designsystem.components

sealed interface StorageChartAnimation {
    data class SegmentBurst(
        val segmentId: String,
    ) : StorageChartAnimation

    data class SegmentReflow(
        val removedSegmentId: String,
        val fromSegments: List<PieSegment>,
        val toSegments: List<PieSegment>,
    ) : StorageChartAnimation

    data object Vortex : StorageChartAnimation
}

const val SEGMENT_BURST_DURATION_MS = 900

const val SEGMENT_REFLOW_DURATION_MS = 750

const val VORTEX_DURATION_MS = 1_200
