export default class SyncRequest {
    ip
    port
    constructor(props) {
        this.ip = props && props.ip
        this.port = props && props.port
    }
}