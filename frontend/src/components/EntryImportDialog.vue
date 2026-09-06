<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { entryApi, type EntryImportPreview } from '../api/entries'
import { errorMessage } from '../api/client'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; committed: [] }>()
const file = ref<File | null>(null)
const preview = ref<EntryImportPreview | null>(null)
const loading = ref(false)
const committing = ref(false)
const error = ref('')
const canCommit = computed(() => Boolean(file.value && preview.value && preview.value.errorRows === 0))
function rowErrors(row: EntryImportPreview['rows'][number]) {
  return Array.isArray(row.errors) ? row.errors : Object.values(row.errors || {})
}
watch(() => props.modelValue, (open) => { if (open) { error.value = ''; preview.value = null; file.value = null } })
function close() { emit('update:modelValue', false) }
function choose(event: Event) {
  const input = event.target as HTMLInputElement
  file.value = input.files?.[0] || null
  preview.value = null
  error.value = ''
}
async function parsePreview() {
  if (!file.value) { error.value = '请选择 UTF-8 CSV 文件'; return }
  loading.value = true; error.value = ''
  try { preview.value = await entryApi.importPreview(file.value) }
  catch (e) { error.value = errorMessage(e, 'CSV 预览失败，请检查文件格式') }
  finally { loading.value = false }
}
async function commit() {
  if (!file.value || !canCommit.value) return
  committing.value = true; error.value = ''
  try { const result = await entryApi.importCommit(file.value, preview.value?.checksum); ElMessage.success(`已导入 ${result.importedRows} 条流水`); emit('committed'); close() }
  catch (e) { error.value = errorMessage(e, '导入失败，未写入任何流水') }
  finally { committing.value = false }
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="导入流水 CSV" width="min(720px, calc(100% - 24px))" @close="close">
    <div class="import-dialog">
      <p class="form-hint">固定列：发生日期、类型、分类、成员编号、金额、备注。最多 500 行，预览全部通过后才能导入。</p>
      <label class="file-picker" for="entry-import-file"><span>选择 CSV 文件</span><input id="entry-import-file" data-test="import-file" type="file" accept=".csv,text/csv" @change="choose" /></label>
      <p v-if="file" class="selected-file" aria-live="polite">已选择：{{ file.name }}</p>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="secondary-button" type="button" :disabled="loading || !file" data-test="preview-import" @click="parsePreview">{{ loading ? '解析中…' : '解析并预览' }}</button>
      <template v-if="preview">
        <div class="import-summary" aria-live="polite"><strong>{{ preview.validRows }}/{{ preview.totalRows }} 行有效</strong><span v-if="preview.errorRows">{{ preview.errorRows }} 行有错误</span><span v-else>全部行可导入</span></div>
        <ul v-if="preview.errorRows" class="import-errors" aria-label="CSV 行错误"><li v-for="row in preview.rows.filter((item) => rowErrors(item).length)" :key="`${row.rowNumber}-${rowErrors(row).join('-')}`">第 {{ row.rowNumber }} 行：{{ rowErrors(row).join('、') }}</li></ul>
        <div v-if="preview.rows.length" class="import-preview-table" role="region" aria-label="CSV 预览"><div v-for="row in preview.rows.slice(0, 10)" :key="row.rowNumber" class="import-preview-row"><span>{{ row.rowNumber }}</span><span>{{ row.occurredOn }}</span><span>{{ row.categoryName }}</span><span>{{ row.amount }}</span></div></div>
      </template>
    </div>
    <template #footer><button class="secondary-button" type="button" @click="close">取消</button><button class="primary-button" type="button" data-test="commit-import" :disabled="committing || !canCommit" @click="commit">{{ committing ? '导入中…' : '确认导入' }}</button></template>
  </el-dialog>
</template>
