/**
 * 仅js端使用 方便快速存取
 */
export default class QuickInfo {
   bookQuery
   imgResourceMap
   cssResourceMap
   htmlResourceMap
   constructor(props) {
      this.bookQuery = props && props.bookQuery
      this.imgResourceMap = props && props.imgResourceMap
      this.cssResourceMap = props && props.cssResourceMap
      this.htmlResourceMap = props && props.htmlResourceMap
   }
}