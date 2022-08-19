module.exports = function (config) {
    config.set({
        browsers: ['ChromeHeadless'],
        // The directory where the output file lives
        //basePath: 'out',
        proxies: {
            "/contracts/build/": "/base/resources/public/contracts/build/"
        },
        // The file itself
        files: ['out/karma-tests.js',
            {pattern: "resources/public/contracts/build/*.json", watched: false, served: true, included: false, nocache: true}],
        frameworks: ['cljs-test'],
        plugins: ['karma-cljs-test', 'karma-chrome-launcher'],
        colors: true,
        logLevel: config.LOG_INFO,
        client: {
            args: ["shadow.test.karma.init"],
            singleRun: true
        }
    })
};
