[![Build Status](https://travis-ci.org/finos-fdx/bot-github-chatops.svg?branch=master)](https://travis-ci.org/finos-fdx/bot-github-chatops)
[![Open Issues](https://img.shields.io/github/issues/finos-fdx/bot-github-chatops.svg)](https://github.com/finos-fdx/bot-github-chatops/issues)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/finos-fdx/bot-github-chatops.svg)](http://isitmaintained.com/project/finos-fdx/bot-github-chatops "Average time to resolve an issue")
[![Dependencies Status](https://versions.deps.co/finos-fdx/bot-github-chatops/status.svg)](https://versions.deps.co/finos-fdx/bot-github-chatops)
[![License](https://img.shields.io/github/license/finos-fdx/bot-github-chatops.svg)](https://github.com/finos-fdx/bot-github-chatops/blob/master/LICENSE)
[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://finosfoundation.atlassian.net/wiki/display/FINOS/Incubating)

<img align="right" width="40%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# bot-github-chatops

A [Symphony](http://www.symphony.com/) bot that uses ChatOps techniques to allow a firm employee to interact in a
compliant manner with GitHub issues and PRs in the [various FINOS GitHub organisations](https://finos.github.io/), via
CLI-esque interactions with the bot.

## Current Status

The [MVP](https://github.com/finos-fdx/bot-github-chatops/projects/1) is effectively complete, and the bot has been
deployed on a pre-production basis to the Foundation's production pod, with cross-pod enabled so that you can interact
with it from any cross-pod enabled Symphony account.

[Feedback, comments, bug reports & enhancement requests are all welcome!](https://github.com/finos-fdx/bot-github-chatops/issues)

## Installation

There is no installation required for the bot, beyond searching for it on the Symphony production network (the bot's
name is "GitHub Bot") and requesting a connection.  The bot automatically accepts all connections requests every 30
minutes, so it may take up to that long to accept your request.

If you have an account on the [Symphony public pod](https://my.symphony.com/), and have already established a connection
to the bot, you may [chat with the bot here](https://my.symphony.com/?embed/&module=im&userIds=73942156967980).

## Usage

The bot will provide you with help by asking it for `help`:

<p align="center">
  <img width="500px" alt="GitHub ChatOps bot help" src="https://raw.githubusercontent.com/finos-fdx/bot-github-chatops/master/bot-github-chatops-example.png"/>
</p>

## Developer Information

[GitHub project](https://github.com/finos-fdx/bot-github-chatops)

[Issue Tracker](https://github.com/finos-fdx/bot-github-chatops/issues)

## Contributing

1. Fork it (<https://github.com/finos-fdx/bot-github-chatops/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool. Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*

## License

The code in this repository is distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

Copyright 2017-2019 Fintech Open Source Foundation
