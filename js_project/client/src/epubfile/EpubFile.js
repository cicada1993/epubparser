import React, { Component } from 'react'
import { observer } from 'mobx-react'
import JSBridge from './JSBridge'
import css from './epubfile.module.css'
const ImageComp = observer((props) => {
    console.log('MutableComp is rendering')
    const { data } = props
    console.log(data)
    return (
        <div><img style={{ width: "80%" }} src={data.curImageData} /></div>
    )
})
export default class EpubFile extends Component {
    ip = '192.168.0.100'
    port = 9595
    bookKey
    chapterIndex
    zipped = true
    fileUrl
    bookUrl
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
            <div className={css.andserver}>
                <div>this is andserver page</div>
                <div><input onChange={(e) => { this.changeBookKey(e) }} placeholder="输入图书Key" /></div>
                <div>
                    <button onClick={() => { this.openBook(true) }}>通过压缩包打开图书</button>
                    <button onClick={() => { this.openBook(false) }}>通过解压目录打开图书</button>
                </div>
                <div><input onChange={(e) => { this.changeChapterIndex(e) }} placeholder="输入章节编号" /></div>
                <button onClick={() => { this.loadChapter() }}>加载章节</button>
                <div><input onChange={(e) => { this.changeFileUrl(e) }} placeholder="输入文件地址" /></div>
                <div>
                    <button onClick={() => { this.downloadFile() }}>下载文件</button>
                    <button onClick={() => { this.openFromServer() }}>直接打开</button>
                </div>
            </div>
        )
    }

    openBook(zipped) {
        const bookKey = this.bookKey
        this.zipped = zipped
        if (zipped) {
            this.bookUrl = `http://${this.ip}:${this.port}/resources/server/book/${bookKey}`
        } else {
            this.bookUrl = `http://${this.ip}:${this.port}/resources/server/book/unzip/${bookKey}/`
        }
        const backUrl = `http://${this.ip}:${this.port}/book/bookResult`
        window.jsBridge.openBook(
            JSON.stringify({
                bookKey,
                bookUrl: this.bookUrl,
                zipped: this.zipped,
                backUrl
            })
        )
    }

    loadChapter() {
        const bookKey = this.bookKey
        const zipped = this.zipped
        const backUrl = `http://${this.ip}:${this.port}/book/chapterResult`
        const chapterIndex = this.chapterIndex
        window.jsBridge.loadChapter(
            JSON.stringify(
                {
                    bookKey,
                    bookUrl: this.bookUrl,
                    zipped,
                    chapterIndex,
                    backUrl
                }
            )
        )
    }

    openFromServer() {
        this.zipped = true
        const bookKey = this.bookKey
        this.bookUrl = this.fileUrl
        const backUrl = `http://${this.ip}:${this.port}/book/bookResult`
        window.jsBridge.openBook(
            JSON.stringify({
                bookKey,
                bookUrl: this.bookUrl,
                zipped: this.zipped,
                backUrl
            })
        )
    }

    downloadFile() {
        const fileUrl = this.fileUrl
        new Promise(function (resolve, reject) {
            window.JSZipUtils.getBinaryContent(fileUrl, {
                progress:(prs) => {
                    console.log(prs)
                },
                callback: (err, data) => {
                    if (err) {
                        reject(err);
                    } else {
                        const book = new window.ePub.Book()
                        book.open(data)
                        book.opened.then(
                            function (res) {
                                console.log(res)
                            },
                            function (err) {
                                console.log("打开书籍出错", err)
                            }
                        ).catch(function (err) {
                            console.log("打开书籍出错", err)
                        })
                        resolve(data);
                    }
                }
            });
        });
    }

    changeBookKey(e) {
        this.bookKey = e.target.value
    }

    changeChapterIndex(e) {
        this.chapterIndex = e.target.value
    }

    changeFileUrl(e) {
        this.fileUrl = e.target.value
    }

    componentDidMount() {
        console.log(css)
        this.onLife(4)
    }

    componentWillUnmount() {
        this.onLife(5)
        delete window.jsBridge
    }
}
