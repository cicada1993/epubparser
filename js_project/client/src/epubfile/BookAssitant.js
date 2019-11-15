import * as localForage from "localforage"
import postcss from 'postcss'
import ReactHtmlParser from 'react-html-parser'
import sizeOf from 'image-size'

import OpfPackage from './book/OpfPackage'
import SpineItem from './book/SpineItem'
import ImageResource from './book/ImageResource'
import HtmlResource from './book/HtmlResource'
import CssResource from './book/CssResource'
import RuleCss from './css/RuleCss'
import QuickInfo from './book/QuickInfo'

import Chapter from './html/Chapter'
import RenderBlock from './html/RenderBlock'
import TextNode from './html/TextNode'
import ImageNode from './html/ImageNode'
import ContainerNode from './html/ContainerNode'
import BookPlot from "./book/BookPlot"
import ImageTodo from "./html/ImageTodo"
export default class BookAssistant {
    quickInfoMap
    resolver
    constructor(props) {
        this.resolver = props && props.resolver
        this.quickInfoMap = new Map()
        localForage.config({
            driver: localForage.WEBSQL,
            name: 'epubfile',
            version: 1.0,
            description: 'save epub file info'
        })
    }

    // 从内存或数据库查找书籍数据
    async queryBook(bookQuery) {
        const bookKey = bookQuery.getBookKey()
        const quickInfo = this.quickInfoMap.get(bookKey)
        const oldBookQuery = quickInfo && quickInfo.bookQuery
        if (oldBookQuery && oldBookQuery.isSame(bookQuery)) {
            console.log('已有查询记录')
            bookQuery = oldBookQuery
        } else {
            console.log('未找到查询记录')
        }
        const cache = bookQuery.opfPackage
        if (cache) {
            bookQuery.hitType = 'cache'
            return cache
        }
        // const dbInfo = await localForage.getItem(bookKey)
        // const dbCache = dbInfo && dbInfo.opfPackage
        // const resource = dbInfo && dbInfo.resource
        // if (dbCache) {
        //     bookQuery.hitType = 'db'
        //     bookQuery.opfPackage = dbCache
        //     bookQuery.resource = resource
        //     return dbCache
        // }
        const originBook = await this.openBook(bookQuery)
        if (originBook) {
            console.log('书籍已打开', originBook)
            bookQuery.originBook = originBook
            return this.rebuildBook(bookQuery)
        }
    }

    async openBook(bookQuery) {
        const { bookRequest: { bookUrl, zipped } } = bookQuery
        const originBook = window.ePub(bookUrl, { openAs: zipped ? 'epub' : 'directory' })
        return originBook.opened
    }

    rebuildBook(bookQuery) {
        const { originBook, bookRequest } = bookQuery
        const { container, package: bookPackage, resources } = originBook
        const { metadata, coverPath, ncxPath, navPath, spine, uniqueIdentifier } = bookPackage
        const { assets, css, html } = resources
        // 重新构建目录列表
        const rbSpine = Array.of()
        for (let item of spine) {
            rbSpine.push(new SpineItem(item))
        }
        // 重构书籍信息
        const bookPlot = new BookPlot({ ...bookRequest })
        const opfPackage = new OpfPackage({
            bookPlot,
            container,
            metadata,
            coverPath,
            navPath,
            ncxPath,
            spine: rbSpine,
            uniqueIdentifier
        })
        bookQuery.opfPackage = opfPackage
        bookQuery.resource = { assets, css, html }
        bookQuery.hitType = 'none'
        return opfPackage
    }

    cacheBookResource(bookQuery) {
        const { hitType } = bookQuery
        if (hitType == 'none') {
            console.log('初次打开，需要解析资源文件')
            const { bookRequest, originBook, resource } = bookQuery
            const { bookKey } = bookRequest
            let quickInfo = this.quickInfoMap.get(bookKey)
            if (quickInfo) {
                quickInfo.bookQuery = bookQuery
            } else {
                quickInfo = new QuickInfo({ bookQuery })
                this.quickInfoMap.set(
                    bookKey,
                    quickInfo
                )
            }
            const { assets, css, html } = resource
            // 提取图片资源
            const imgResourceMap = new Map()
            for (let file of assets) {
                if (file.type.indexOf('image') != -1) {
                    const imgName = this.getNameByHref(file.href)
                    const imageResource = new ImageResource({
                        name: imgName, ...file
                    })
                    imgResourceMap.set(
                        imgName,
                        imageResource
                    )
                }
            }
            // 提取css资源
            const cssResourceMap = new Map()
            for (let file of css) {
                const cssName = this.getNameByHref(file.href)
                const cssResource = new CssResource({
                    name: cssName, ...file
                })
                cssResourceMap.set(
                    cssName,
                    cssResource
                )
                this.parseCss(originBook, cssResource)
            }
            // 提取html资源
            const htmlResourceMap = new Map()
            for (let file of html) {
                const htmlName = this.getNameByHref(file.href)
                const htmlResource = new HtmlResource({
                    name: htmlName, ...file
                })
                htmlResourceMap.set(
                    htmlName,
                    htmlResource
                )
                this.parseHtml(originBook, htmlResource)
            }
            quickInfo.imgResourceMap = imgResourceMap
            quickInfo.cssResourceMap = cssResourceMap
            quickInfo.htmlResourceMap = htmlResourceMap
        }
    }

    // 保存书籍信息到数据库
    cacheBookToDB(bookQuery) {
        const { opfPackage, resource } = bookQuery
        const { bookPlot: { bookKey } } = opfPackage
        localForage.setItem(bookKey, { opfPackage, resource }).then(
            () => {
                console.log('书籍信息缓存到数据库')
            }
        )
    }

    handleImageTodos(originBook, imageTodos) {
        const length = imageTodos.length
        return new Promise((resolve, reject) => {
            let remain = length
            for (let i = 0; i < length; i++) {
                const { imageResource, imageNode } = imageTodos[i]
                const href = imageResource.href
                originBook.loadImage(href).then(
                    (imageData) => {
                        remain--
                        const buffer = Buffer.from(imageData, "base64")
                        const dimension = sizeOf(buffer)
                        console.log(`image resolved ${href}`, dimension)
                        imageResource.base64Data = imageData
                        imageResource.dimension = dimension
                        imageResource.decoded = true
                        imageNode.href = href
                        imageNode.base64Data = imageData
                        imageNode.width = dimension.width
                        imageNode.height = dimension.height
                        if (remain == 0) {
                            console.log('解析完成')
                            resolve(true)
                        }
                        const base64Uri = `data:image/${dimension.type};base64,${imageData}`
                        this.resolver.setImageData(base64Uri)
                    }
                )
            }
        });
    }

    parseCss(originBook, cssResource) {
        const href = cssResource.href
        originBook.loadText(href).then(
            (cssData) => {
                console.log('cssData back', href)
                const root = postcss.parse(cssData)
                const rules = Array.of()
                root.walkRules(rule => {
                    rules.push(new RuleCss(rule))
                })
                cssResource.rules = rules
            }
        )
    }

    parseHtml(originBook, htmlResource) {
        const href = htmlResource.href
        originBook.loadText(href).then(
            (htmlData) => {
                console.log('htmlData back', href)
                const doc = ReactHtmlParser(htmlData)
                if (Array.isArray(doc)) {
                    let htmlNode
                    for (let node of doc) {
                        if (node && node.type === 'html') {
                            htmlNode = node
                            break
                        }
                    }
                    htmlResource.htmlNode = htmlNode
                }
            }
        )
    }

    async queryChapter(chapterQuery) {
        const bookKey = chapterQuery.getBookKey()
        const chapterIndex = chapterQuery.getChapterIndex()
        const quickInfo = this.quickInfoMap.get(bookKey)
        const { bookQuery, htmlResourceMap } = quickInfo
        const { spine } = bookQuery.opfPackage
        const spineItem = spine[chapterIndex]
        const { href } = spineItem
        const htmlName = this.getNameByHref(href)
        const htmlResource = htmlResourceMap.get(htmlName)
        let renderBlocks = htmlResource && htmlResource.renderBlocks
        if (Array.isArray(renderBlocks)) {
            chapterQuery.hitType = 'cache'
            return new Chapter({ bookKey, chapterIndex, renderBlocks })
        }
        const htmlNode = htmlResource && htmlResource.htmlNode
        const htmlProps = htmlNode && htmlNode.props
        const htmlChildren = htmlProps && htmlProps.children
        // 找到有效根节点
        let validRootNode
        for (let node of htmlChildren) {
            if (node && node.type === 'body') {
                validRootNode = node
                break
            }
        }
        const rootProps = validRootNode && validRootNode.props
        const rootChildren = rootProps && rootProps.children
        if (Array.isArray(rootChildren) && rootChildren.length >= 1) {
            // 将section节点作为body节点
            const onlyChild = rootChildren[0]
            if (onlyChild && onlyChild.type === 'section') {
                validRootNode = onlyChild
            }
            const { chapterRequest } = chapterQuery
            const bookPlot = new BookPlot({ ...chapterRequest })
            const imageTodos = Array.of()
            renderBlocks = this.getRenderBlocks(imageTodos, bookPlot, validRootNode)
            if (imageTodos.length > 0) {
                console.log('有未解析的图片', imageTodos)
                await this.handleImageTodos(bookQuery.originBook, imageTodos)
            }
            htmlResource.renderBlocks = renderBlocks
        }
        chapterQuery.hitType = 'none'
        return new Chapter({ bookKey, chapterIndex, renderBlocks })
    }

    /**
     * 解析html标签
     * @param {*} rootReactNode 
     */
    getRenderBlocks(imageTodos, bookPlot, rootReactNode) {
        const blocks = Array.of()
        const props = rootReactNode && rootReactNode.props
        const children = props && props.children
        const length = children && children.length
        for (let blockIndex = 0; blockIndex < length; blockIndex++) {
            const blockNode = this.resolve(imageTodos, bookPlot, children[blockIndex])
            if (blockNode) {
                blocks.push(
                    new RenderBlock({
                        blockIndex,
                        blockNode
                    })
                )
            }
        }
        return blocks
    }

    resolve(imageTodos, bookPlot, reactNode) {
        const type = reactNode && reactNode.type
        const props = reactNode.props
        const children = props && props.children
        if (typeof reactNode === 'string') {
            // 文本节点
            return this.resolveTextNode(imageTodos, bookPlot, reactNode)
        } else if (type === 'img') {
            // 图片节点
            return this.resolveImageNode(imageTodos, bookPlot, reactNode)
        } else if (type === 'image') {
            // svg内的图片节点
            return this.resolveSvgImageNode(imageTodos, bookPlot, reactNode)
        } else if (Array.isArray(children) && children.length >= 1) {
            // 容器节点
            return this.resolveContainerNode(imageTodos, bookPlot, reactNode)
        } else {
            console.log('未知节点类型', reactNode)
        }
    }

    resolveTextNode(imageTodos, bookPlot, content) {
        return new TextNode({ bookPlot, content })
    }

    resolveImageNode(imageTodos, bookPlot, reactNode) {
        const type = reactNode.type
        const props = reactNode.props
        const className = props && props.className
        const style = props && props.style
        const alt = props && props.alt
        const active = props && props.active
        const src = props && props.src
        const imageNode = new ImageNode({
            bookPlot,
            type,
            className,
            style,
            alt,
            active,
            src
        })
        // 得到图片路径
        const imageName = this.getNameByHref(src)
        const { bookKey } = bookPlot
        const quickInfo = this.quickInfoMap.get(bookKey)
        const { imgResourceMap } = quickInfo
        const imageResource = imgResourceMap.get(imageName)
        if (imageResource.decoded) {
            const { href, base64Data, dimension: { width, height } } = imageResource
            imageNode.href = href
            imageNode.base64Data = base64Data
            imageNode.width = width
            imageNode.height = height
        } else {
            imageTodos.push(new ImageTodo({ imageResource, imageNode }))
        }
        return imageNode
    }

    resolveSvgImageNode(imageTodos, bookPlot, reactNode) {
        const type = reactNode.type
        const props = reactNode.props
        const href = props && props['xlink:href']
        const imageNode = new ImageNode({
            bookPlot,
            type,
            href
        })
        // 得到图片路径
        const imageName = this.getNameByHref(href)
        const { bookKey } = bookPlot
        const quickInfo = this.quickInfoMap.get(bookKey)
        const { imgResourceMap } = quickInfo
        const imageResource = imgResourceMap.get(imageName)
        if (imageResource.decoded) {
            const { href, base64Data, dimension: { width, height } } = imageResource
            imageNode.href = href
            imageNode.base64Data = base64Data
            imageNode.width = width
            imageNode.height = height
        } else {
            imageTodos.push(new ImageTodo({ imageResource, imageNode }))
        }
        return imageNode
    }

    resolveContainerNode(imageTodos, bookPlot, reactNode) {
        const type = reactNode.type
        const props = reactNode.props
        const className = props && props.className
        const style = props && props.style
        const children = props && props.children
        const childNodes = Array.of()
        for (let child of children) {
            const resolveNode = this.resolve(imageTodos, bookPlot, child)
            if (resolveNode) {
                childNodes.push(resolveNode)
            }
        }
        return new ContainerNode({
            bookPlot,
            type,
            className,
            style,
            children: childNodes
        })
    }

    /**
    * 通过路径获取文件名
    * @param {*} href 
    */
    getNameByHref(href) {
        const arr = href.split('/')
        let name
        if (Array.isArray(arr)) {
            const length = arr.length
            name = arr[length - 1]
        }
        return name
    }

}