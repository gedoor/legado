import type { Source } from '@/source'
import bookSourceEditConfig from './bookSourceEditConfig'
import rssSourceEditConfig from './rssSourceEditConfig'

type PickAnyValueKey<T> = {
  [K in keyof T]: T[K] extends { [prop: string]: string } ? K : never
}
type b = keyof PickAnyValueKey<BookSoure>

type SourceConfigKey =
  | keyof typeof bookSourceEditConfig
  | keyof typeof rssSourceEditConfig
type SourceConfigRecord = {
  title: string
  type: string //"array" | "String" | "Boolean"
  array?: string[]
  hint?: string
  required?: boolean
  namespace?: Partial<keyof Source>
  id: Partial<keyof Source>
}
type SourceConfigValue = { name: string; children: SourceConfigRecord[] }
export type SourceConfig = Partial<Record<SourceConfigKey, SourceConfigValue>>
