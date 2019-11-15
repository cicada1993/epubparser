import { decorate, observable, computed, autorun } from 'mobx'
import Fly from 'flyio/dist/npm/fly'

import BookQuery from './query/BookQuery'
import ChapterQuery from './query/ChapterQuery'
import BookRequest from './request/BookRequest'
import ChapterRequest from './request/ChapterRequest'
import BookAssistant from './BookAssitant'
import SyncRequest from './request/SyncRequest'
class JSBridge {
    imageData
    requestData
    bookAssistant
    constructor() {
        autorun(
            () => {
                console.log('curRequestData', this.curRequestData)
            }
        )
        this.bookAssistant = new BookAssistant({ resolver: this })
    }

    get curRequestData() {
        return this.requestData
    }

    get curImageData() {
        return this.imageData
    }

    syncServer(requestStr) {
        this.requestData = requestStr
        const bookRequest = new SyncRequest({...JSON.parse(requestStr)})
    }

    openBook(requestStr) {
        this.requestData = requestStr
        const bookRequest = new BookRequest({...JSON.parse(requestStr)})
        const bookQuery = new BookQuery({ bookRequest })
        this.bookAssistant.queryBook(bookQuery).then(
            (result) => {
                this.onOpenResult(true, { bookQuery, result })
            },
            (err) => {
                this.onOpenResult(false, { bookQuery, err })
            }
        ).catch((err) => {
            this.onOpenResult(false, { bookQuery, err })
        })
    }

    loadChapter(requestStr) {
        this.requestData = requestStr
        const chapterRequest = new ChapterRequest({...JSON.parse(requestStr)})
        const chapterQuery = new ChapterQuery({ chapterRequest })
        this.bookAssistant.queryChapter(chapterQuery).then(
            (result) => {
                this.onChapterResult(true, { chapterQuery, result })
            },
            (err) => {
                this.onChapterResult(false, { chapterQuery, err })
            }
        ).catch((err) => {
            this.onChapterResult(false, { chapterQuery, err })
        })
    }

    onOpenResult(success, { bookQuery, result, err }) {
        let data
        if (success) {
            data = JSON.stringify(result)
            console.log('书籍打开成功', result)
        } else {
            data = JSON.stringify(err)
            console.log('书籍打开失败', err)
        }
        const { bookRequest: { bookKey, backUrl } } = bookQuery
        const fly = new Fly()
        fly.post(backUrl, { success, bookKey, data })
            .then((response) => {
                console.log(response.data)
                if (success) {
                    this.bookAssistant.cacheBookResource(bookQuery)
                }
            })
            .catch((error) => {
                console.log(error)
            })
    }

    onChapterResult(success, { chapterQuery, result, err }) {
        let data
        if (success) {
            data = JSON.stringify(result)
            console.log('章节加载成功', result)
        } else {
            data = JSON.stringify(err)
            console.log('书籍打开失败', err)
        }
        const { chapterRequest: { bookKey, chapterIndex, backUrl } } = chapterQuery
        const fly = new Fly()
        fly.post(backUrl, { success, bookKey, chapterIndex, data })
            .then((response) => {
                console.log(response.data)
            })
            .catch((error) => {
                console.log(error)
            })
    }

    setImageData(data) {
        this.imageData = data
    }
}

decorate(JSBridge, {
    imageData: observable,
    requestData: observable,
    curRequestData: computed,
    curImageData: computed
})

export default JSBridge