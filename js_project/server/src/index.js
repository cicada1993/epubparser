const express = require('express')
const cors = require('cors')
const fs = require('fs')
const path = require('path')
const serveIndex = require('serve-index')
const app = express()
const bodyParser = require('body-parser')
const multer = require('multer')
const upload = multer({ dest: './upload' })
app.use(bodyParser.json())
app.use(bodyParser.urlencoded({ extended: true }))
// 处理跨域
app.use(cors())
// However, the path that you provide to the express.static function is relative 
// to the directory from where you launch your node process（这里指的是根目录的index.js）. 
// If you run the express app from another directory, 
// it’s safer to use the absolute path of the directory that you want to serve:
// 使得静态资源目录可在浏览器中查看
app.use('/resources/server', serveIndex('./public', { icons: true }))
// 服务端静态资源
app.use('/resources/server', express.static('./public'))
// 部署客户端静态资源
app.use('/client', serveIndex('../client/build', { icons: true }))
app.use('/client', express.static('../client/build'))
app.get('/', (req, res) => {
    res.send('hello, express')
})

app.post('/', (req, res) => {

})

app.put('/', (req, res) => {

})

app.delete('/', (req, res) => {

})

app.post('/book/openResult',(req,res) => {
    res.send(
        {
            success:true
        }
    )
})

app.post('/book/chapterResult',(req,res) => {
    res.send(
        {
            success:true
        }
    )
})

app.post('/uploadFile', upload.single('file'), (req, res) => {
    if (req.file.length === 0) {
        res.send({ success: false, msg: 'file can not be empty' })
    } else {
        const file = req.file
        fs.renameSync('./upload/' + file.filename, './upload/' + file.originalname)
        const fileInfo = {}
        // 获取文件信息
        fileInfo.mimetype = file.mimetype;
        fileInfo.originalname = file.originalname;
        fileInfo.size = file.size;
        fileInfo.path = file.path;
        console.log(fileInfo)
        res.send({ success: true, msg: 'upload success' })
    }
})

app.listen(9595, () => {
    console.log('express app listening on port 9595')
})


