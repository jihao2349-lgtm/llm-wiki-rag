<script setup lang="ts">
import { computed } from "vue"
import { NTag } from "naive-ui"
import type { SourceStatus, TaskStatus } from "../types"
import { sourceStatusType, taskStatusLabel, taskStatusType } from "../utils/status"

const props = defineProps<{
  status: SourceStatus | TaskStatus
}>()

const tagType = computed(() => {
  if (["Pending", "Processing", "Done", "Failed", "Cancelled"].includes(props.status)) {
    return taskStatusType(props.status as TaskStatus)
  }

  return sourceStatusType(props.status as SourceStatus)
})

const label = computed(() => {
  if (["Pending", "Processing", "Done", "Failed", "Cancelled"].includes(props.status)) {
    return taskStatusLabel(props.status as TaskStatus)
  }

  return props.status
})
</script>

<template>
  <NTag :type="tagType" round :bordered="false">{{ label }}</NTag>
</template>
