var serverStatus = {
    check: function (callback) {
        $.get("/status", callback);
    },
    lastStatus : "AVAILABLE",
    start: function (selector) {
        setInterval(function () {
            serverStatus.check(function (result) {
                $(selector).html(result);
                serverStatus.lastStatus = result;
            })
        }, 1000);

    }
};

String.prototype.replaceAll = function(search, replacement) {
    var target = this;
    return target.replace(new RegExp(search, 'g'), replacement);
};