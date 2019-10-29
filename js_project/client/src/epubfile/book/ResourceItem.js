/**
 * 资源文件
 * [href] 相对路径
 * [type]文件类型
 */
export default class ResourceItem {
    href
    type
    constructor(props) {
        this.href = props && props.href
        this.type = props && props.type
    }
}