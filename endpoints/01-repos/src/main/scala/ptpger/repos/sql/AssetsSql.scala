package ptpger.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import ptpger.domain.Asset
import ptpger.domain.AssetId

private[repos] object AssetsSql extends Sql[AssetId] {
  private val codec: Codec[Asset] = (id *: zonedDateTime *: nes *: bool *: nes.opt).to[Asset]
  val insert: Command[Asset] =
    sql"""INSERT INTO assets VALUES ($codec)""".command

  def getByIds(assetIds: List[AssetId]): Query[assetIds.type, Asset] =
    sql"""SELECT * FROM assets WHERE id in ${id.values.list(assetIds)}""".query(codec)
}
