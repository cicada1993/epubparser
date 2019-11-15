/**
 * 目录项
 * [cfiBase] epubcfi
 * [href] 资源相对路径
 * [idref] 资源id
 * [index] 序号
 * [linear]
 */
export default class SpineItem {
    cfiBase
    href
    idref
    index
    linear
    constructor(props) {
        this.cfiBase = props && props.cfiBase
        this.href = props && props.href
        this.idref = props && props.idref
        this.index = props && props.index
        this.linear = props && props.linear
    }
}