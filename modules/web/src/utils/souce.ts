import type { BookSoure, RssSource, Source } from '../source'
import { isNullOrBlank } from './utils'

const isBookSource = (source: Source): source is BookSoure =>
  'bookSourceName' in source

export const isInvaildSource: (source: Source) => boolean = source => {
  if (isBookSource(source)) {
    return (
      !isNullOrBlank(source.bookSourceName) &&
      !isNullOrBlank(source.bookSourceUrl) &&
      !isNullOrBlank(source.bookSourceType)
    )
  }
  return !isNullOrBlank(source.sourceName) && !isNullOrBlank(source.sourceUrl)
}

export const getSourceUniqueKey = (source: Source) =>
  isBookSource(source) ? source.bookSourceUrl : source.sourceUrl
export const getSourceName = (source: Source) =>
  isBookSource(source) ? source.bookSourceName : source.sourceName

export const isSourceMatches: (source: Source, searchKey: string) => boolean = (
  source,
  searchKey,
) => {
  // TODO: 正则和普通字符串识别 识别 * . \ [ ] <= <! != = ?: () \d\w\s\...
  if (isBookSource(source)) {
    return (
      (source.bookSourceName.includes(searchKey) ||
        source.bookSourceUrl.includes(searchKey) ||
        source.bookSourceGroup?.includes(searchKey) ||
        source.bookSourceComment?.includes(searchKey)) ??
      false
    )
  }
  return (
    (source.sourceName.includes(searchKey) ||
      source.sourceUrl.includes(searchKey) ||
      source.sourceGroup?.includes(searchKey) ||
      source.sourceComment?.includes(searchKey)) ??
    false
  )
}

export const convertSourcesToMap = (sources: Source[]): Map<string, Source> => {
  const map = new Map()
  sources.forEach(source => map.set(getSourceUniqueKey(source), source))
  return map
}

export const emptyBookSource = {
  ruleSearch: {},
  ruleBookInfo: {},
  ruleToc: {},
  ruleContent: {},
  ruleReview: {},
  ruleExplore: {},
} as BookSoure
export const emptyRssSource = {} as RssSource
