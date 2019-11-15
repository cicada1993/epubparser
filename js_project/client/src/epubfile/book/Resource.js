/**
 * 资源文件
 * [name] 文件名
 * [href] 相对路径
 * [type]文件类型
 */
export default class Resource {
    name
    href
    type
    constructor(props) {
        this.name = props && props.name
        this.href = props && props.href
        this.type = props && props.type
    }
}