import Resource from './Resource'
// 图片资源文件
export default class ImageResource extends Resource {
    base64Data
    dimension
    decoded
    constructor(props) {
        super(props)
        this.base64Data = props && props.base64Data
        this.dimension = props && props.dimension
    }
}