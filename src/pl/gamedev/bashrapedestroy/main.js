dojo.ready(function() {
	client.init();
});

window["client"] = {
	init : function() {
		client.timeOffset = new Date().getTime();
		dojo.xhrPost({
			url : '/join',
			content : [client.timeOffset],
			sync : true,
			load : function(a) {
				a = eval('(' + a + ')');
				client["ticket"] = a.t;
				client.timeOffset = a.ts;
			}
		});

		dojo.body().innerHTML = '<div id="view" style="background: #577452; width: 100%; height: 100%; position: fixed; overflow: hidden"><input type="textbox" id="input" style="position: absolute; left: -1000px; top: -1000ppx"/></div>';
		this["view"] = dojo.byId('view');
		this["input"] = dojo.byId('input');
		this["objects"] = {};

		dojo.connect(this.input, 'onkeydown', this, "onkeydown");
		dojo.connect(this.input, 'onkeyup', this, "onkeyup");

		this.focusInterval = setInterval("client.input.focus()", 100);

		this["player"] = new client.Warrior(100, 100);
		this.objects[this.ticket] = this.player;
		// this["cloud"] = new client.Warrior(100, 100);

		this["lastTime"] = new Date().getTime();
		this.download();
		this.upload(true);
		this.update();
	},
	sprites : {
		'warrior' : {
			url : 'sprites.png',
			offsetX : 0,
			offsetY : 0,
			width : 32,
			height : 64,
			zIndex : 1
		},
		'shadow' : {
			url : 'sprites.png',
			offsetX : 0,
			offsetY : -192,
			width : 32,
			height : 16,
			zIndex : 0
		},
		'cloud' : {
			url : 'sprites2.png',
			offsetX : 0,
			offsetY : 0,
			width : 128,
			height : 128,
			zIndex : 2
		}
	},
	onkeydown : function(event) {
		if (event.keyCode == 87)
			this.player.setDirection(null, -1) || (this.upload());
		else if (event.keyCode == 83)
			this.player.setDirection(null, 1) || (this.upload());
		else if (event.keyCode == 65)
			this.player.setDirection(-1, null) || (this.upload());
		else if (event.keyCode == 68)
			this.player.setDirection(1, null) || (this.upload());
		// else if(event.keyCode == 81)
		// this.player.setDirection(-1, -1);
		// else if(event.keyCode == 69)
		// this.player.setDirection(1, -1);

		else if (event.keyCode == 32)
			this.player.jump() || (this.upload());
	},
	onkeyup : function(event) {
		if (event.keyCode == 87 || event.keyCode == 83)
			this.player.setDirection(null, 0) || (this.upload());
		else if (event.keyCode == 65 || event.keyCode == 68)
			this.player.setDirection(0, null) || (this.upload());
		// else if(event.keyCode == 69 || event.keyCode == 81)
		// this.player.setDirection(0, 0);
	},
	update : function() {
		var time = new Date().getTime();
		var dt = time - this.lastTime;

		// this.player.update(dt / 1000);
		for ( var id in this.objects)
			this.objects[id].update(dt / 1000);
		// if(Math.random() > 0.5) {
		// this.cloud.column = Math.floor(Math.random() * 4);
		// this.cloud.render();
		// }

		this.lastTime = time;
		setTimeout("client.update()", Math.max(50, 0));
	},
	upload : function(force) {
		if (!force)
			return (this.uploadTimer = 0);
		if (!this.uploadTimer || new Date().getTime() - this.uploadTimer >= 100) {
			this.uploadTimer = new Date().getTime();
			dojo.xhrPost({
				url : '/input',
				sync : true,
				content : [this.ticket, this.player.object.x,
						this.player.object.y, this.player.object.z,
						this.player.fx, this.player.fy, this.player.fz,
						this.player.h, this.player.v],
				load : function(a) {
				}
			});
		}
		this.uploadTimeout = setTimeout("client.upload(true);", 10);
	},
	download : function() {
		dojo.xhrGet({
			url : '/im'+ Math.floor((this.lastTime + this.timeOffset) / 100),
			sync : true,
			headers : {
				"Content-Type" : 'text/plain; charset=x-user-defined'
			},
			load : function(data) {
				data = data.split('\n');
				// console.log(data);
				var nobjects = parseInt(data[0]);
				for ( var i = 0; i < nobjects; i++) {
					var id = parseInt(data[1 + i * 9]);
					if (!client.objects[id])
						client.objects[id] = new client.Warrior(256, 256);
					var obj = client.objects[id];
					if (id != client.ticket) {
						obj.object.x = parseFloat(data[2 + i * 9]);
						obj.object.y = parseFloat(data[3 + i * 9]);
						obj.object.z = parseFloat(data[4 + i * 9]);
						obj.fx = parseFloat(data[5 + i * 9]);
						obj.fy = parseFloat(data[6 + i * 9]);
						obj.fz = parseFloat(data[7 + i * 9]);
						obj.setDirection(parseInt(data[8 + i * 9]),
								parseInt(data[9 + i * 9]));
					}
					// if(!client.objects)
					// client.objects[id].object.x = x;
					// client.objects[id].object.y = y;
				}
			},
			error : function() {
			}
		});
		setTimeout("client.download();", 100);
	}

};

client["Object"] = function(x, y, z, spriteIdx) {
	this["x"] = parseFloat(x);
	this["y"] = parseFloat(y);
	this["z"] = parseFloat(z);
	this["column"] = 0;
	this["row"] = 0;
	this["el"] = document.createElement('div');
	this["sprite"] = client.sprites[spriteIdx];
	
	dojo.style(this.el, {
		position : 'absolute',
		left : this.x + 'px',
		top : this.y - this.z + 'px',
		background : 'url(' + this.sprite.url + ')',
		width : this.sprite.width + 'px',
		height : this.sprite.height + 'px',
		zIndex : Math.floor(this.sprite.zIndex * 1000 + this.y)
	});

	this["render"] = function() {
		dojo.style(this.el, {
			left : this.x + 'px',
			top : this.y - this.z + 'px',
			zIndex : Math.floor(this.sprite.zIndex * 1000 + this.y)
		});
		this.el.style.backgroundPosition = (this.sprite.offsetX - (this.column * this.sprite.width))
				+ 'px '
				+ (this.sprite.offsetY - (this.row * this.sprite.height))
				+ 'px';
	};

	this.render();
	client.view.appendChild(this.el);
};

client["Warrior"] = function(x, y) {
	this["fx"] = 0;
	this["fy"] = 0;
	this["fz"] = 0;

	this["h"] = 0;
	this["v"] = 0;
	this["rowc"] = 0;

	this["object"] = new client.Object(x, y, 0, 'warrior');
	this["shadow"] = new client.Object(x, y, 0, 'shadow');
	this["setDirection"] = function(h, v) {
		if (h != null)
			this.h = h;
		if (v != null)
			this.v = v;
	}

	this["jump"] = function() {
		if (this.object.z == 0)
			this.fz = 100;
	};

	this["update"] = function(dt) {
		if (this.object.z > 0)
			this.fz = Math.max(this.fz - 100 * dt, -1000);
		else {
			var dir = Math.atan2(this.v, this.h);
			if (this.h || this.v) {
				this.fx = Math.cos(dir);
				this.fy = Math.sin(dir);
			}
		}

		if (this.fx != 0)
			this.fx = this.fx - this.fx * dt * 10 / Math.max(this.object.z, 1);
		if (this.fy != 0)
			this.fy = this.fy - this.fy * dt * 10 / Math.max(this.object.z, 1);

		if (this.fz > 0)
			this.fz = this.fz - this.fz * dt;
		this.object.z = this.object.z + this.fz * dt;
		if (this.object.z < 0) {
			this.object.z = 0;
			this.object.fz = 0;
		}
		this.shadow.x = (this.object.x += this.fx * dt * 100);
		this.shadow.y = (this.object.y += this.fy * dt * 100)
				+ this.object.sprite.height - this.shadow.sprite.height / 2;
		this.object.column = this.h > 0 ? 1 : this.h < 0 ? 2 : this.v > 0
				? 0
				: 3;
		if (Math.abs(this.fx) + Math.abs(this.fy) + Math.abs(this.fz) > 0)
			this.object.row = 1 + Math.floor(this.rowc += Math.sqrt(Math.pow(
					this.fx, 2)
					+ Math.pow(this.fy, 2))
					* dt * 5) % 2;
		else
			this.object.row = 0;
		this.object.render();
		this.shadow.render();
	}
};