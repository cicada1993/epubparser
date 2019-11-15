/**
 * 图书整体信息
 * [bookKey] 图书唯一标识
 * [bookPath] 图书相对路径
 * [bookUnzipPath] 图书解压资源相对路径
 * [container] META-INF/container.xml文件信息
 * [metadata] opf文件中metadata标签内容
 * [coverPath] 封面路径
 * [navPath]
 * [ncxPath] opf文件中spine标签指明的ncx文件路径
 * [spine] 解析ncx得到的目录列表
 * [uniqueIdentifier]资源唯一编号
 */
export default class OpfPackage {
    bookPlot
    container
    metadata
    coverPath
    navPath
    ncxPath
    spine
    uniqueIdentifier
    constructor(props) {
        this.bookPlot = props && props.bookPlot
        this.container = props && props.container
        this.metadata = props && props.metadata
        this.coverPath = props && props.coverPath
        this.navPath = props && props.navPath
        this.ncxPath = props && props.ncxPath
        this.spine = props && props.spine
        this.uniqueIdentifier = props && props.uniqueIdentifier
    }
}