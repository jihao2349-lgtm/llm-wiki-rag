<script setup lang="ts">
import AppIcon from "./AppIcon.vue"
import type { Metric } from "../types"

defineProps<{
  metric: Metric
}>()
</script>

<template>
  <div class="metric-card" :class="`metric-card--${metric.tone}`">
    <div class="metric-card__head">
      <span class="metric-card__label">{{ metric.label }}</span>
      <div v-if="metric.icon" class="metric-card__icon">
        <AppIcon :name="metric.icon" :size="16" />
      </div>
    </div>
    <div class="metric-card__value">{{ metric.value }}</div>
    <span class="metric-card__desc">{{ metric.description }}</span>
  </div>
</template>
