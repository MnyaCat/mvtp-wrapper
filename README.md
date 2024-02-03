# Mvtp-Wrapper

Multiverse-Coreの`mvtp`コマンドをラップするBukkit系プラグインです。

## コマンド

### `/mvtp-<WORLDNAME>`

各ワールドへの移動コマンドです。このコマンドはプラグインが動的に登録します。
`survival`ワールドと`creative`ワールドがある場合、`mvtp-survival`と`mvtp-creative`が登録されます。

#### 使用方法

`/<command>, /<command> [player], /<command> [player] [default|force|spawn]`

#### 例

`/mvtp-<WORLDNAME>`, `/mvtp-<WORLDNAME> player`, `/mvtp-<WORLDNAME> spawn`

#### エイリアス

`/mvtp-<WORLDNAME>...`

#### パーミッション

`mvtpwrapper.teleportworld.<WORLDNAME>`
`mvtpwrapper.teleport.self`
`mvtpwrapper.teleport.other`

#### 説明

ワールド間をテレポートするコマンドです。前回そのワールドでいたディメンション、座標へテレポートします。
テレポート先の座標が安全ではない(空中や溶岩に触れる)場合は、周囲の安全な場所へテレポートします。周囲に安全な場所が見つからない場合、テレポートは失敗します。

ワールドへテレポートするには、`mvtpwrapper.teleportworld.<WORLDNAME>`が必要です。
自分をテレポートするには`mvtpwrapper.teleport.self`が、自分以外をテレポートするには`mvtpwrapper.teleport.other`が必要です。

- **\[player]**: テレポートするプレイヤーを指定します。
- **\[default|force|spawn]**: テレポートモードを指定します。

テレポートモードで`force`を指定すると、テレポート先が安全かどうかに関わらず強制的にテレポートします。`spawn`を指定すると、スポーン地点へテレポートします。`default`は通常の動作(前回いた座標へのテレポート)です。

## 前提プラグイン

- [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core)
- [Multiverse-NetherPortals](https://github.com/Multiverse/Multiverse-NetherPortals)

## LICENSE

This software is released under the MIT License, see [LICENSE.txt](LICENSE.txt).
