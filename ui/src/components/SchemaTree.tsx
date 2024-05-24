import React from "react";
import {RichTreeView} from "@mui/x-tree-view";

interface SchemasTreeProps {

}

export const SchemaTree: React.FC<SchemasTreeProps> = props => {

  return (
    <RichTreeView items={[]}></RichTreeView>
  )
}
