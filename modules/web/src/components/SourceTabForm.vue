<template>
  <el-tabs id="source-edit">
    <el-tab-pane
      v-for="{ name, children } in Object.values(config)"
      :label="name"
      :key="name"
    >
      <el-form label-position="right" label-width="auto">
        <el-form-item
          v-for="{
            type,
            title,
            namespace,
            id,
            array,
            hint,
            required,
          } in children"
          :label="title"
          :key="title"
          :required="required"
        >
          <el-input
            v-if="type == 'String' && typeof namespace == 'undefined'"
            type="textarea"
            v-model="currentSource[id]"
            :placeholder="hint"
            autosize
          />
          <el-input
            v-if="type == 'String' && typeof namespace != 'undefined'"
            type="textarea"
            v-model="currentSource[namespace][id]"
            :placeholder="hint"
            autosize
          />

          <el-switch v-if="type == 'Boolean'" v-model="currentSource[id]" />

          <el-input-number
            v-if="type == 'Number'"
            v-model="currentSource[id]"
            :min="0"
          />

          <el-select v-if="type == 'Array'" v-model="currentSource[id]">
            <el-option
              v-for="(name, index) in array"
              :value="index"
              :key="name"
              :label="name"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
const store = useSourceStore();
defineProps(["config"]);
const { currentSource } = storeToRefs(store);
</script>

<style lang="scss" scoped>
:deep(.el-tab-pane) {
  height: calc(100vh - 55px);
  padding-top: 15px;
  padding-right: 5px;
  overflow-y: auto;
}
:deep(.el-tabs__header) {
  margin: 0;
}
</style>
