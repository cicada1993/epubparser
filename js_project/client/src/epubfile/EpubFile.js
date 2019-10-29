import React, { Component } from 'react'
import './epubfile.css'
import JSBridge from './JSBridge'

export default class EpubFile extends Component {
    bookKey
    chapterIndex
    constructor(props) {
        super(props)
        window.jsBridge = new JSBridge()
        this.onLife(1)
    }

    /**
     * 向客户端同步组件生命周期
     * @param {生命周期类型}} lifeType 
     */
    onLife(lifeType) {
        if (window.platform && window.platform.onLife) {
            window.platform.onLife(lifeType)
        }
    }

    componentWillMount() {
        this.onLife(2)
    }

    render() {
        this.onLife(3)
        return (
            <div className='andserver'>
                <div>this is andserver page</div>
                <div><input onChange={(e) => { this.changeBookKey(e) }} placeholder="输入图书Key" /></div>
                <button onClick={() => { this.openBook() }}>打开图书</button>
                <div><input onChange={(e) => { this.changeChapterIndex(e) }} placeholder="输入章节编号" /></div>
                <button onClick={() => { this.loadChapter() }}>加载章节</button>
            </div>
        )
    }

    openBook() {
        const ip = '192.168.56.156'
        const port = 9595
        const bookKey = this.bookKey
        const bookPath = `/resources/server/book/${this.bookKey}`
        const bookUnzipPath = `/resources/server/book/unzip/${this.bookKey}/`
        window.jsBridge.openBook(
            JSON.stringify({
                ip,
                port,
                bookKey,
                bookPath,
                bookUnzipPath
            })
        )
    }

    loadChapter() {
        const ip = '192.168.56.156'
        const port = 9595
        const bookKey = this.bookKey
        const chapterIndex = this.chapterIndex
        window.jsBridge.loadChapter(
            JSON.stringify(
                {
                    ip,
                    port,
                    bookKey,
                    chapterIndex
                }
            )
        )
    }

    changeBookKey(e) {
        this.bookKey = e.target.value
    }

    changeChapterIndex(e) {
        this.chapterIndex = e.target.value
    }

    componentDidMount() {
        this.onLife(4)
    }

    componentWillUnmount() {
        this.onLife(5)
        delete window.jsBridge
    }
}
