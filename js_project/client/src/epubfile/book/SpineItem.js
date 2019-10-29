/**
 * 目录项
 * [canonical]
 * [cfiBase] epubcfi
 * [idref] 资源id
 * [index] 序号
 * [linear]
 * [url] 资源url
 */
export default class SpineItem {
    canonical
    cfiBase
    href
    idref
    index
    linear
    url
    constructor(props) {
        this.canonical = props && props.canonical
        this.cfiBase = props && props.cfiBase
        this.href = props && props.href
        this.idref = props && props.idref
        this.index = props && props.index
        this.linear = props && props.linear
        this.url = props && props.url
    }
}