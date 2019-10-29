import { decorate, observable, computed, autorun } from 'mobx'
import * as localForage from "localforage"
import OpfPackage from './book/OpfPackage'
import ResourceItem from './book/ResourceItem'
import SpineItem from './book/SpineItem'
import Fly from 'flyio/dist/npm/fly'
import ReactHtmlParser from 'react-html-parser'
import ContainerItem from './render/ContainerItem'
import TextItem from './render/TextItem'
import ImageItem from './render/ImageItem'
class JSBridge {
    baseURL
    bookMap
    constructor() {
        autorun(() => {
            console.log('baseURL', this.baseURL)
        })
        this.bookMap = new Map()
        localForage.config({
            driver: localForage.WEBSQL,
            name: 'epubfile',
            version: 1.0,
            description: 'save epub file info'
        })
    }

    get provideBaseURL() {
        return this.baseURL
    }

    get provideBookMap() {
        return this.bookMap
    }

    /**
     * 打开图书
     * @param {*} param 
     */
    openBook(param) {
        console.log(param)
        const openRequest = JSON.parse(param)
        const { ip, port, bookKey, bookPath, bookUnzipPath } = openRequest
        this.baseURL = `http://${ip}:${port}`
        this.queryBook(bookKey, bookPath, bookUnzipPath).then(
            (book) => {
                console.log('书籍信息', book)
                this.onOpenResult(true, bookKey, JSON.stringify(book))
            },
            (err) => {
                console.log('书籍打开失败', err)
                this.onOpenResult(false, bookKey, JSON.stringify(err))
            }
        ).catch((err) => {
            console.log('书籍打开失败', err)
            this.onOpenResult(false, bookKey, JSON.stringify(err))
        })
    }

    /**
     * 加载章节数据
     * @param {*} param 
     */
    loadChapter(param) {
        console.log(param)
        const chapterRequest = JSON.parse(param)
        const { ip, port, bookKey, chapterIndex } = chapterRequest
        this.baseURL = `http://${ip}:${port}`
        this.queryChapter(bookKey, chapterIndex).then(
            (paragraphs) => {
                console.log('段落列表', paragraphs)
                this.onChapterResult(true, bookKey, JSON.stringify(paragraphs))
            },
            (err) => {
                console.log('章节获取失败', err)
                this.onChapterResult(false, bookKey, JSON.stringify(err))
            }
        ).catch((err) => {
            console.log('章节获取失败', err)
            this.onChapterResult(false, bookKey, JSON.stringify(err))
        })
    }


    /**
     * 查找图书信息
     * @param {*} bookKey 
     * @param {*} bookPath 
     * @param {*} bookUnzipPath 
     */
    async queryBook(bookKey, bookPath, bookUnzipPath) {
        const cache = await this.getBookFromCache(bookKey)
        if (cache) {
            return cache
        }
        const bookUnzipUrl = `${this.provideBaseURL}/${bookUnzipPath}`
        const book = await this.createBook(bookUnzipUrl)
        if (book) {
            console.log('书籍已打开', book)
            return await this.storeBookInfo(bookKey, bookPath, bookUnzipPath, book)
        }
    }

    async getBookFromCache(bookKey) {
        const memoryCache = this.bookMap.get(bookKey)
        if (memoryCache) {
            console.log('命中内存缓存')
            return memoryCache
        }
        const dbCache = await localForage.getItem(bookKey)
        if (dbCache) {
            console.log('命中数据库缓存')
            this.bookMap.set(bookKey, dbCache)
            return dbCache
        }
    }

    /**
   * 打开书籍
   * @param {图书解压后的目录地址} bookUnzipUrl 
   */
    createBook(bookUnzipUrl) {
        const book = new window.ePub.Book()
        book.open(bookUnzipUrl)
        return book.opened
    }

    /**
     * 从原始Book对象中提取主要信息后保存
     * @param {*} bookKey 
     * @param {*} bookPath 
     * @param {*} bookUnzipPath 
     * @param {*} book 
     */
    storeBookInfo(bookKey, bookPath, bookUnzipPath, book) {
        const { container, package: packageInfo, resources, spine } = book
        const { metadata, coverPath, ncxPath, navPath, uniqueIdentifier } = packageInfo
        const { resources: allResources } = resources
        const { spineItems } = spine
        const opfPackage = new OpfPackage()
        opfPackage.bookKey = bookKey
        opfPackage.bookPath = bookPath
        opfPackage.bookUnzipPath = bookUnzipPath
        opfPackage.container = container
        opfPackage.resources = this.rebuildResources(allResources)
        opfPackage.metadata = metadata
        opfPackage.coverPath = coverPath
        opfPackage.navPath = navPath
        opfPackage.ncxPath = ncxPath
        opfPackage.spine = this.rebuildSpineItems(spineItems)
        opfPackage.uniqueIdentifier = uniqueIdentifier
        this.bookMap.set(bookKey, opfPackage)
        console.log('书籍缓存到内存')
        return localForage.setItem(bookKey, opfPackage).then(
            () => {
                console.log('书籍缓存到数据库')
                return localForage.getItem(bookKey)
            }
        )
    }

    /**
     * 重新构建资源列表
     * @param {*} allResources 
     */
    rebuildResources(allResources) {
        const array = new Array()
        for (let resource of allResources) {
            array.push(new ResourceItem(resource))
        }
        return array
    }

    /**
     * 重新构建目录列表
     * @param {*} spineItems 
     */
    rebuildSpineItems(spineItems) {
        const array = new Array()
        for (let item of spineItems) {
            array.push(new SpineItem(item))
        }
        return array
    }

    /**
     * 根据章节编号查找内容
     * @param {*} bookKey 
     * @param {*} chapterIndex 
     */
    async queryChapter(bookKey, chapterIndex) {
        const book = await this.getBookFromCache(bookKey)
        if (book) {
            const { container, bookUnzipPath, spine } = book
            const { directory } = container
            const spineItem = spine[chapterIndex]
            const { href } = spineItem
            const fly = new Fly()
            const baseURL = this.provideBaseURL
            const response = await fly.get(`${bookUnzipPath}/${directory}/${href}`, {}, { baseURL })
            const html = response.data
            const doc = ReactHtmlParser(html)
            console.log('doc', doc)
            if (Array.isArray(doc)) {
                let htmlNode
                for (let node of doc) {
                    // 找到html节点
                    if (node && node.type === 'html') {
                        htmlNode = node
                        break
                    }
                }
                if (htmlNode) {
                    console.log('html node', htmlNode)
                    const props = htmlNode.props
                    const children = props && props.children
                    if (Array.isArray(children)) {
                        let bodyNode
                        for (let node of children) {
                            // 找到body节点
                            if (node && node.type === 'body') {
                                bodyNode = node
                                break
                            }
                        }
                        if(bodyNode) {
                            console.log('body node', bodyNode)
                            return this.createParagraphs(bodyNode)
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析body节点
     * @param {*} bodyNode 
     */
    createParagraphs(bodyNode) {
        if (bodyNode) {
            const paragraphs = Array.of()
            const props = bodyNode.props
            const children = props && props.children
            if (children) {
                for (let node of children) {
                    paragraphs.push(this.createContainerItem(node))
                }
            }
            return paragraphs
        }
    }

    /**
     * 创建段落元素
     * @param {*} paragraphNode 
     */
    createContainerItem(paragraphNode) {
        const type = paragraphNode.type
        const props = paragraphNode.props
        const className = props && props.className
        const style = props && props.style
        const children = props && props.children
        const renderItems = Array.of()
        if (type === 'img') {
            // 图片元素
            const alt = props && props.alt
            const active = props && props.active
            const src = props && props.src
            renderItems.push(
                new ImageItem({
                    type, className, style, alt, active, src
                })
            )
        } else if (Array.isArray(children) && children.length >= 1) {
            for (let child of children) {
                if (typeof child === 'string') {
                    // 文本元素
                    renderItems.push(
                        new TextItem({
                            content: child
                        })
                    )
                } else if (child.type === 'img') {
                    // 图片元素
                    const childType = child.type
                    const childProps = child.props
                    const childClassName = childProps && childProps.className
                    const childStyle = childProps && childProps.style
                    const childAlt = childProps && childProps.alt
                    const childActive = childProps && childProps.active
                    const childSrc = childProps && childProps.src
                    renderItems.push(
                        new ImageItem({
                            type: childType,
                            className: childClassName,
                            style: childStyle,
                            alt: childAlt,
                            active: childActive,
                            src: childSrc
                        })
                    )
                } else {
                    // 容器元素
                    renderItems.push(this.createContainerItem(child))
                }

            }
        }
        return new ContainerItem({
            type, className, style, children: renderItems
        })
    }


    /**
     * 返回书籍打开结果
     * @param {*} success 
     * @param {*} bookKey 
     * @param {*} data 
     */
    onOpenResult(success, bookKey, data) {
        const fly = new Fly()
        const baseURL = this.provideBaseURL
        fly.post('/book/openResult', { success, bookKey, data }, { baseURL })
            .then((response) => {
                console.log(response.data)
            })
            .catch((error) => {
                console.log(error)
            })
    }

    onChapterResult(success, bookKey, data) {
        const fly = new Fly()
        const baseURL = this.provideBaseURL
        fly.post('/book/chapterResult', { success, bookKey, data }, { baseURL })
            .then((response) => {
                console.log(response.data)
            })
            .catch((error) => {
                console.log(error)
            })
    }
}

decorate(JSBridge, {
    baseURL: observable,
    bookMap: observable,
    provideBaseURL: computed,
    provideBookMap: computed
})

export default JSBridge